package org.litesoft.wip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

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
        String releaseTimestamp = normalizeTimestamp( parts[1] );
        String error = validateTagVersion( tagVersion );
        if ( error != null ) {
            return error( noVersionDataCurrentTimeSupplier, path, "fileErrorTagVersion", error, " from '", tagVersion, "'" );
        }
        error = validateReleaseTimestamp( releaseTimestamp );
        if ( error != null ) {
            return error( noVersionDataCurrentTimeSupplier, path, "fileErrorReleaseTimestamp", error, " from '", releaseTimestamp, "'" );
        }
        return new Version( tagVersion, releaseTimestamp );
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

    private static String validateReleaseTimestamp( String value ) {
        return null; // TODO: XXX
    }

    // normalize to the minute Zulu
    // 2022-07-27T16:38+00:00Z
    // 01234567-101234567-20123
    // 2022-07-27T16:38Z
    private static String normalizeTimestamp( String value ) {
        return value; // TODO: XXX
    }

    private static String toISO_8601z( LongSupplier timeSupplier ) {
        String str = Instant.ofEpochMilli( timeSupplier.getAsLong() ).truncatedTo( ChronoUnit.MINUTES ).toString();
        return str;
    }

    private static String checkDigits( String level, String value, int minDigits, int maxDigits ) {
        return checkDigits( level, value, minDigits, maxDigits, 0, Integer.MAX_VALUE );
    }

    private static String checkDigits( String level, String value, int minDigits, int maxDigits, int minValue, int maxValue ) {
        return null; // TODO: XXX
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
