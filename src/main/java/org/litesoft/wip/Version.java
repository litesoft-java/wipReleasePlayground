package org.litesoft.wip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import org.litesoft.utils.ISO8601ZtimeStamp;

public class Version {
    public static final String RESOURCE_PATH = "version.txt";
    private static final AtomicReference<Version> singleton = new AtomicReference<>();

    public static String get() {
        Version version = singleton.get();
        if ( version == null ) {
            singleton.set( version = from( loadVersionFile( getVersionFileStream( RESOURCE_PATH ), RESOURCE_PATH ),
                                           RESOURCE_PATH, System::currentTimeMillis ) );
            System.out.println( "Version: " + version );
        }
        return version.toString();
    }

    private final String tagVersion;
    private final String releaseTimestamp;

    Version( String tagVersion, String releaseTimestamp ) {
        this.tagVersion = tagVersion;
        this.releaseTimestamp = releaseTimestamp;
    }

    @Override
    public String toString() {
        return releaseTimestamp + " " + tagVersion;
    }

    @SuppressWarnings("SameParameterValue")
    static String loadVersionFile( InputStream is, String path ) {
        if ( is == null ) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try ( InputStreamReader streamReader = new InputStreamReader( is, StandardCharsets.UTF_8 );
              BufferedReader reader = new BufferedReader( streamReader ) ) {

            for ( String line; (line = reader.readLine()) != null; ) {
                sb.append( line ).append( '\n' );
            }
        }
        catch ( IOException e ) {
            new IllegalStateException( "Unable to read " + path, e ).printStackTrace();
            return ""; // empty string indicates
        }
        String str = sb.toString();
        return str.isEmpty() ? null : str;
    }

    @SuppressWarnings("SameParameterValue")
    static InputStream getVersionFileStream( String path ) {
        return Version.class.getClassLoader().getResourceAsStream( path );
    }

    @SuppressWarnings("SameParameterValue")
    static Version from( String fileBody, String path, LongSupplier noVersionDataCurrentTimeSupplier ) {
        if ( fileBody == null ) {
            return error( noVersionDataCurrentTimeSupplier, path, "noFile" );
        }
        if ( fileBody.isEmpty() ) {
            return error( noVersionDataCurrentTimeSupplier, path, "fileLoadError" );
        }
        String[] parts = fileBody.trim().split( " ", 3 );
        if ( parts.length < 2 ) {
            return error( noVersionDataCurrentTimeSupplier, path, "fileErrorParts", "" + parts.length );
        }
        String tagVersion = parts[0];
        String releaseTimestamp = parts[1];
        String error = validateTagVersion( tagVersion );
        if ( error != null ) {
            return error( noVersionDataCurrentTimeSupplier, path, "fileErrorTagVersion", error, " from '", tagVersion, "'" );
        }
        ISO8601ZtimeStamp timeStamp = ISO8601ZtimeStamp.fromString( releaseTimestamp ).toMinute();
        if ( timeStamp.hasError() ) {
            return error( noVersionDataCurrentTimeSupplier, path, "fileErrorReleaseTimestamp", error, " from '", releaseTimestamp, "'" );
        }
        return new Version( tagVersion, timeStamp.getValue() );
    }

    private static String validateTagVersion( String value ) {
        if ( !value.startsWith( "v" ) ) {
            return "did not start with a 'v'";
        }
        String[] parts = value.substring( 1 ).split( "\\.", 4 );
        if ( parts.length != 3 ) {
            return "not 3 parts";
        }
        String error = checkDigits( "MAJOR", parts[0], 2, 2 );
        if ( error == null ) {
            error = checkDigits( "MINOR", parts[1], 1, 2, 1, 12 );
            if ( error == null ) {
                error = checkDigits( "PATCH", parts[1], 1, 5 );
            }
        }
        return error;
    }

    private static String toISO_8601z( LongSupplier timeSupplier ) {
        return ISO8601ZtimeStamp.fromEpochMillis( timeSupplier ).toMinute().getValue();
    }

    private static String checkDigits( String level, String value, int minDigits, int maxDigits ) {
        return checkDigits( level, value, minDigits, maxDigits, 0, Integer.MAX_VALUE );
    }

    private static String checkDigits( String level, String value, int minDigits, int maxDigits, int minValue, int maxValue ) {
        if ( value.isBlank() ) {
            return "no " + level;
        }
        if ( !value.equals( value.trim() ) ) {
            return level + " value '" + value + "' has whitespace";
        }
        int len = value.length();
        if ( len < minDigits ) {
            return level + " value '" + value + "' too short, expected " + minDigits + " digits, but only got: " + len;
        }
        if ( maxDigits < len ) {
            return level + " value '" + value + "' too long, expected " + maxDigits + " digits, but got: " + len;
        }
        int parsed;
        try {
            parsed = Integer.parseInt( value );
        }
        catch ( NumberFormatException e ) {
            return level + " value '" + value + "' not an integer";
        }
        if ( parsed < 0 ) {
            return level + " value '" + value + "' NOT a non-negative value, got: " + parsed;
        }
        if ( parsed < minValue ) {
            return level + " value '" + value + "' too small, expected at least " + minDigits + ", but only got: " + parsed;
        }
        if ( maxValue < parsed ) {
            return level + " value '" + value + "' too large, expected no more than " + maxValue + ", but got: " + parsed;
        }
        return null;
    }

    private static Version error( LongSupplier timeSupplier, String path, String errorType, String... what ) {
        StringBuilder sb = new StringBuilder().append( errorType );
        if ( (what != null) && (what.length != 0) ) {
            sb.append( '(' );
            for ( String str : what ) {
                sb.append( str );
            }
            sb.append( ')' );
        }
        sb.append( '@' ).append( path );
        return new Version( sb.toString(), toISO_8601z( timeSupplier ) );
    }
}
