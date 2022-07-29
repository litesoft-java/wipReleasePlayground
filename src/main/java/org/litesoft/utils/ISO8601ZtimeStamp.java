package org.litesoft.utils;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Class to except and manage an ISO-8601(ish) timestamp which is mapped to Zulu/UTC and conforms with the following:
 * <p><ul>
 * <li> only positive years and the year is assumed to include the century and not exceed 9999,
 * <li> uses dashes, '-', for date field separators,
 * <li> uses colons, ':', for time field separators, AND
 * <li> uses a 'T' to separate the time fields from the date fields!
 * </ul><p>
 * Date validation assumes the current Gregorian rules re Leap Days, and does not accept Leap Seconds.<p>
 * Note: Since Gregorian rules do not factor in the different countries transition from pre-Gregorian systems (e.g. Julian),
 * for England and America, at least, dates before 1800 may not map to actual dates as the period peoples use them!
 */
@SuppressWarnings("unused")
public class ISO8601ZtimeStamp {
    private final String value;
    private final String error;

    private ISO8601ZtimeStamp( String value, String error ) {
        this.value = value;
        this.error = error;
    }

    private ISO8601ZtimeStamp( String value ) {
        this( value, null );
    }

    public String getValue() {
        return value;
    }

    public String getError() {
        return error;
    }

    public boolean hasError() {
        return (getError() != null);
    }

    public ISO8601ZtimeStamp toHour() {
        return adjustTo( TimeLength.Hour );
    }

    public ISO8601ZtimeStamp toMinute() {
        return adjustTo( TimeLength.Minute );
    }

    public ISO8601ZtimeStamp toSecond() {
        return adjustTo( TimeLength.Second );
    }

    public ISO8601ZtimeStamp toMillis() {
        return adjustTo( TimeLength.Millis );
    }

    public ISO8601ZtimeStamp toMicros() {
        return adjustTo( TimeLength.Micros );
    }

    public ISO8601ZtimeStamp toNanos() {
        return adjustTo( TimeLength.Nanos );
    }

    public static ISO8601ZtimeStamp now() {
        return fromEpochMillis( System.currentTimeMillis() );
    }

    public static ISO8601ZtimeStamp fromEpochMillis( LongSupplier supplier ) {
        return fromEpochMillis( supplier.getAsLong() );
    }

    public static ISO8601ZtimeStamp fromEpochMillis( long millis ) {
        return new ISO8601ZtimeStamp( Instant.ofEpochMilli( millis ).toString() );
    }

    /**
     * Attempt to map the ISO-8601(ish) string into a UTC/Zulu form.
     *
     * @param iso8601ish to parse
     * @return instance, possibly with an error (and hence a bad value)!
     */
    public static ISO8601ZtimeStamp fromString( String iso8601ish ) {
        if ( iso8601ish == null ) {
            return new ISO8601ZtimeStamp( null, "null" );
        }
        iso8601ish = iso8601ish.trim().toUpperCase();
        if ( iso8601ish.isEmpty() ) {
            return new ISO8601ZtimeStamp( iso8601ish, "empty" );
        }
        int at = iso8601ish.indexOf( 'T' );
        if ( at == -1 ) {
            return new ISO8601ZtimeStamp( iso8601ish, "no date time seperator 'T'" );
        }
        Date date = new Date().parse( iso8601ish.substring( 0, at ) );
        if ( date.hasError() ) {
            return new ISO8601ZtimeStamp( iso8601ish, date.error );
        }
        Time time = new Time().parse( iso8601ish.substring( at + 1 ) ).normalize( date );
        if ( time.hasError() ) {
            return new ISO8601ZtimeStamp( iso8601ish, time.error );
        }
        return new ISO8601ZtimeStamp( date + "T" + time );
    }
    // Some ISO-ish forms:
    // 2022-07-27T16:38+00:00Z
    // 01234567-101234567-20123
    // 2022-07-27T16:38Z-07
    // 2022-07-27T16:38:34.123456789-01:45
    // 01234567-101234567-201234567-3012345

    private static class Date {
        private String error;
        private int year;
        private int month;
        private int day;

        public boolean hasError() {
            return error != null;
        }

        public String toString() {
            if ( hasError() ) {
                return "error: " + error;
            }
            StringBuilder sb = new StringBuilder();
            FourDigits.addTo( sb, year ).append( '-' );
            TwoDigits.addTo( sb, month ).append( '-' );
            TwoDigits.addTo( sb, day );
            return sb.toString();
        }

        private void setError( String error ) {
            if ( !hasError() ) {
                this.error = error;
            }
        }

        private Date withError( String error ) {
            setError( error );
            return this;
        }

        public Date parse( String value ) {
            String[] dateFields = value.split( "-" );
            if ( dateFields.length != 3 ) {
                return withError( "incorrect number of date fields, expected 3, but got " + dateFields.length );
            }
            year = parseField( dateFields[0], "year", 0, 9999 );
            month = parseField( dateFields[1], "month", 1, 12 );
            if ( !hasError() ) {
                day = parseField( dateFields[2], "day", 1, GregorianMonth.from( month ).getDaysInMonth( year ) );
            }
            return this;
        }

        private int parseField( String field, String what, int min, int max ) {
            return parseValue( field.trim(), what, "date field", min, max, this::setError );
        }

        private void decrementYear() {
            year--;
            if ( !hasError() && (year < 0) ) {
                withError( "year not allowed to be negative" );
            }
        }

        private void incrementYear() {
            year++;
            if ( !hasError() && (9999 < year) ) {
                withError( "year not allowed to exceed 4 digits" );
            }
        }

        private void decrementMonth() {
            month--;
            if ( month < 1 ) {
                decrementYear();
                month = 12;
            }
        }

        private void incrementMonth() {
            month++;
            if ( 12 < month ) {
                incrementYear();
                month = 1;
            }
        }

        public void decrementDay() {
            day--;
            if ( day < 1 ) {
                decrementMonth();
                day = maxDayOfMonth();
            }
        }

        public void incrementDay() {
            day++;
            if ( maxDayOfMonth() < day ) {
                incrementMonth();
                day = 1;
            }
        }

        public int maxDayOfMonth() {
            if ( (month < 1) || (12 < month) ) {
                return -1;
            }
            return GregorianMonth.from( month ).getDaysInMonth( year );
        }
    }

    private static class Time {
        private String error;
        private int hour;
        private int minute;
        private int second;
        private int millis;
        private int micros;
        private int nanos;
        private int offsetHours;
        private int offsetMinutes;

        public boolean hasError() {
            return error != null;
        }

        public String toString() {
            if ( hasError() ) {
                return "error: " + error;
            }
            StringBuilder sb = new StringBuilder();
            TwoDigits.addTo( sb, hour ).append( '-' );
            TwoDigits.addTo( sb, minute ).append( '-' );
            TwoDigits.addTo( sb, second );
            if ( nanos != 0 ) {
                ThreeDigits.addTo( sb.append( '.' ), millis, micros, nanos );
            } else if ( micros != 0 ) {
                ThreeDigits.addTo( sb.append( '.' ), millis, micros );
            } else if ( millis != 0 ) {
                ThreeDigits.addTo( sb.append( '.' ), millis );
            }
            if ( (offsetHours == 0) && (offsetMinutes == 0) ) {
                return sb.append( 'Z' ).toString();
            }
            if ( (offsetHours > 0) || (offsetMinutes > 0) ) {
                return addOffset( sb.append( '+' ), offsetHours, offsetMinutes );
            }
            return addOffset( sb.append( '-' ), -offsetHours, -offsetMinutes );
        }

        private void setError( String error ) {
            this.error = error;
        }

        private Time withError( String error ) {
            setError( error );
            return this;
        }

        private int withError_minus1( String error ) {
            this.error = error;
            return -1;
        }

        public Time normalize( Date date ) {
            if ( !hasError() ) {
                hour += offsetHours;
                minute += offsetMinutes;
                offsetHours = offsetMinutes = 0;
                if ( minute < 0 ) {
                    hour--;
                    minute += 60;
                }
                if ( 60 <= minute ) {
                    hour++;
                    minute -= 60;
                }
                if ( hour < 0 ) {
                    date.decrementDay();
                    hour += 24;
                }
                if ( 24 <= hour ) {
                    hour++;
                    date.incrementDay();
                }
            }
            return this;
        }

        public Time parse( String value ) {
            int offsetsAt = parseOffsets( value );
            return hasError() ? this : parseTimeFields( value.substring( 0, offsetsAt ).split( ":" ) );
        }

        private Time parseTimeFields( String[] timeFields ) {
            if ( timeFields.length > 3 ) {
                return withError( "too many time fields, expected at most 3, but got " + timeFields.length );
            }
            hour = parseTimeField( extract( timeFields, 0 ), "hours", 23 );
            minute = parseTimeField( extract( timeFields, 1 ), "minutes", 59 );
            String secondsField = extract( timeFields, 2 );
            if ( secondsField != null ) {
                int decimalAt = secondsField.indexOf( '.' );
                if ( decimalAt != -1 ) {
                    parseFractionalSecs( secondsField.substring( decimalAt + 1 ) );
                    secondsField = secondsField.substring( 0, decimalAt );
                }
                second = parseTimeField( secondsField, "seconds", 59 );
            }
            return this;
        }

        private String extract( String[] timeFields, int index ) {
            return (index < timeFields.length) ? timeFields[index].trim() : null;
        }

        private int parseTimeField( String field, String what, int max ) {
            return parseValue( field, what, "time field", 0, max, this::setError );
        }

        private void parseFractionalSecs( String fraction ) {
            if ( fraction.length() > 9 ) {
                withError( "fractional seconds longer than 9 (digits)" );
            } else {
                millis = parseFraction( fraction, 0, "millis" );
                micros = parseFraction( fraction, 3, "micros" );
                nanos = parseFraction( fraction, 6, "nanos" );
            }
        }

        private int parseFraction( String fullFraction, int offset, String what ) {
            int value = 0;
            if ( offset < fullFraction.length() ) {
                String fraction = fullFraction.substring( offset );
                switch ( fraction.length() ) {
                    case 1:
                        fraction += "0";
                        // fall thru
                    case 2:
                        fraction += "0";
                        // fall thru
                    case 3:
                        break;
                    default:
                        fraction = fraction.substring( 0, 3 );
                }
                value = parseValue( fraction, "Second", what, 0, 999, this::setError );
            }
            return Math.max( 0, value );
        }

        private int parseOffsets( String value ) {
            int offsetsAt = findOffsets( value );
            if ( hasError() ) {
                return -1;
            }
            int zAt = value.indexOf( 'Z' );
            if ( (offsetsAt == -1) && (zAt == -1) ) {
                return withError_minus1( "no 'Z' or offset" );
            }
            if ( offsetsAt == -1 ) { // happy case, just a 'Z'
                return checkPostZ( zAt, value );
            }
            // Offset exists!
            if ( zAt != -1 ) { // a Z AND an offsets
                if ( zAt < offsetsAt ) { // offset after 'Z' means we can ignore it (idea by GAS & GitHub).
                    return checkPostZ( zAt, value.substring( 0, offsetsAt ) );
                }
                // offset then Z, means Z is meaningless!
                checkPostZ( zAt, value ); // ensure nothing after Z
                if ( hasError() ) {
                    return -1;
                }
                value = value.substring( 0, zAt ); // drop Z
            }
            // An offset exists so parse!
            String[] offsets = value.substring( offsetsAt + 1 ).split( ":", 3 );
            switch ( offsets.length ) {
                case 2:
                    offsetMinutes = validateOffsetMinutes( parseOffset( offsets[1], "minutes", 45 ) );
                    // Fall thru
                case 1:
                    offsetHours = parseOffset( offsets[0], "hours", 14 );
                    break;
                default:
                    return withError_minus1( "expected at most one colon in offset, but found more in '" +
                                             value.substring( offsetsAt ) + "'" );
            }
            if ( value.charAt( offsetsAt ) == '-' ) {
                offsetHours = -offsetHours;
                offsetMinutes = -offsetMinutes;
            }
            return offsetsAt;
        }

        private int validateOffsetMinutes( int minOffset ) {
            return switch ( minOffset ) {
                case 0, 15, 30, 45 -> minOffset;
                default -> withError_minus1( "minute offset, not a quarter hour, but was " + minOffset );
            };
        }

        private int parseOffset( String value, String what, int max ) {
            return parseValue( value, what, "offset", 0, max, this::setError );
        }

        private int checkPostZ( int zAt, String value ) {
            String postZ = value.substring( zAt + 1 ).trim();
            return postZ.isEmpty() ? zAt : withError_minus1( "'" + postZ + "' following 'Z'" );
        }

        private int findOffsets( String value ) {
            int negOffsetAt = value.indexOf( '-' );
            int posOffsetAt = value.indexOf( '+' );
            if ( negOffsetAt == -1 ) {
                return posOffsetAt;
            }
            if ( posOffsetAt == -1 ) {
                return negOffsetAt;
            }
            return withError_minus1( "both a negative and positive offset" );
        }
    }

    private interface ErrorConsumer extends Consumer<String> {
    }

    private static int parseValue( String strValue, String what, String type, int min, int max, ErrorConsumer ec ) {
        strValue = strValue.trim();
        int intValue;
        try {
            intValue = Integer.parseInt( strValue );
        }
        catch ( NumberFormatException e ) {
            return withError_minus1( ec, what + " " + type + " of '" + strValue + "' -- parse error" );
        }
        if ( intValue < min ) {
            return withError_minus1( ec, what + " " + type + " of '" + strValue + "' -- les than min value of " + min );
        }
        if ( intValue > max ) {
            return withError_minus1( ec, what + " " + type + " of '" + strValue + "' -- exceeded max value of " + max );
        }
        return intValue;
    }

    private static int withError_minus1( ErrorConsumer ec, String error ) {
        ec.accept( error );
        return -1;
    }

    private static String addOffset( StringBuilder sb, int offsetHours, int offsetMinutes ) {
        TwoDigits.addTo( sb, offsetHours );
        if ( offsetMinutes != 0 ) {
            TwoDigits.addTo( sb.append( ':' ), offsetMinutes );
        }
        return sb.toString();
    }

    @SuppressWarnings("UnusedReturnValue")
    private static class Digits {
        private final Digits next;
        private final int valueExtractor;

        public Digits( Digits next ) {
            this.next = next;
            valueExtractor = (next == null) ? 1 : (next.valueExtractor * 10);
        }

        public StringBuilder addTo( StringBuilder sb, int value ) {
            char digit = '0';
            for ( ; value > valueExtractor; value -= valueExtractor ) {
                digit++;
            }
            sb.append( digit );
            return next.addTo( sb, value );
        }

        public StringBuilder addTo( StringBuilder sb, int value0, int... moreValues ) {
            addTo( sb, value0 );
            for ( int valueN : moreValues ) {
                addTo( sb, valueN );
            }
            return sb;
        }
    }

    private static final Digits OneDigit = new Digits( null ) {
        @Override
        public StringBuilder addTo( StringBuilder sb, int value ) {
            return sb.append( (char)('0' + value) );
        }
    };
    private static final Digits TwoDigits = new Digits( OneDigit );
    private static final Digits ThreeDigits = new Digits( TwoDigits );
    private static final Digits FourDigits = new Digits( ThreeDigits );

    private ISO8601ZtimeStamp adjustTo( TimeLength desiredTL ) {
        if ( hasError() ) {
            return this;
        }
        String value = getValue();
        TimeLength currentTL = TimeLength.from( value );
        if ( desiredTL == currentTL ) { // Happy case!
            return this; // No change
        }
        if ( currentTL == null ) {
            return new ISO8601ZtimeStamp( value, "no matching TimeLength" );
        }
        return new ISO8601ZtimeStamp( desiredTL.adjust( value ) );
    }

    private static final String EXAMPLE_TIME_STAMP =
            "yyyy-mm-ddT00:00:00.000000000Z"; // 30 long
    // len:  1234567-101234567-20123456789

    enum TimeLength {
        Hour( 13 ),
        Minute( 16 ),
        Second( 19 ),
        Millis( 23 ),
        Micros( 26 ),
        Nanos( 29 );

        private final int expectedZlessLength;

        TimeLength( int zLessLength ) {
            expectedZlessLength = zLessLength;
        }

        public String adjust( String iso8601z ) {
            int lessZLength = iso8601z.length() - 1;
            String newBase = (expectedZlessLength < lessZLength) ?
                             iso8601z.substring( 0, expectedZlessLength ) :
                             iso8601z.substring( 0, lessZLength )
                             + EXAMPLE_TIME_STAMP.substring( lessZLength + 1, expectedZlessLength );
            return newBase + "Z";
        }

        public static TimeLength from( String iso8601z ) {
            if ( (iso8601z != null) && iso8601z.endsWith( "Z" ) ) {
                int lessZLength = iso8601z.length() - 1;
                if ( (Hour.expectedZlessLength <= lessZLength) && (iso8601z.charAt( 10 ) == 'T') ) {
                    for ( TimeLength tl : values() ) {
                        if ( tl.expectedZlessLength == lessZLength ) {
                            return tl;
                        }
                    }
                }
            }
            return null;
        }
    }
}
