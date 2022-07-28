package org.litesoft.utils;

import java.time.Instant;
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
public class ISO8601Z {
    private final String value;
    private final String error;

    private ISO8601Z( String value, String error ) {
        this.value = value;
        this.error = error;
    }

    private ISO8601Z( String value ) {
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

    public static ISO8601Z now() {
        return fromEpochMillis( System.currentTimeMillis() );
    }

    public static ISO8601Z fromEpochMillis( LongSupplier supplier ) {
        return fromEpochMillis( supplier.getAsLong() );
    }

    public static ISO8601Z fromEpochMillis( long millis ) {
        return new ISO8601Z( Instant.ofEpochMilli( millis ).toString() );
    }

    /**
     * Attempt to map the ISO-8601(ish) string into a UTC/Zulu form.
     *
     * @param iso8601ish to parse
     * @return instance, possibly with an error (and hence a bad value)!
     */
    public static ISO8601Z fromString( String iso8601ish ) {
        if ( iso8601ish == null ) {
            return new ISO8601Z( iso8601ish, "null" );
        }
        iso8601ish = iso8601ish.trim().toUpperCase();
        if ( iso8601ish.isEmpty() ) {
            return new ISO8601Z( iso8601ish, "empty" );
        }
        int at = iso8601ish.indexOf( 'T' );
        if ( at == -1 ) {
            return new ISO8601Z( iso8601ish, "no date time seperator 'T'" );
        }
        Date date = new Date().parse( iso8601ish.substring( 0, at ) );
        if ( date.hasError() ) {
            return new ISO8601Z( iso8601ish, date.error );
        }
        Time time = new Time().parse( iso8601ish.substring( at + 1 ) ).normalize( date );
        if ( time.hasError() ) {
            return new ISO8601Z( iso8601ish, time.error );
        }
        return new ISO8601Z( date.toString() + "T" + time.toString() );
    }
    // Some ISO forms:
    // 2022-07-27T16:38+00:00Z
    // 01234567-101234567-20123
    // 2022-07-27T16:38Z
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

        private Date withError( String error ) {
            this.error = error;
            return this;
        }

        public Date parse( String value ) {
            return this;
        }

        public void decrementDay() {
            day--;
            if (day < 1) {
                decrementMonth();
                day = maxDayOfMonth();
            }
        }

        public void incrementDay() {
            day++;
            if (maxDayOfMonth() < day) {
                incrementMonth();
                day = 1;
            }
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

        private Time withError( String error ) {
            this.error = error;
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
                if (minute < 0) {
                    hour--;
                    minute += 60;
                }
                if (60 <= minute) {
                    hour++;
                    minute -= 60;
                }
                if (hour < 0) {
                    date.decrementDay();
                    hour += 24;
                }
                if (24 <= hour) {
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
            int value;
            try {
                value = Integer.parseInt( field );
            }
            catch ( NumberFormatException e ) {
                return withError_minus1( what + " time field of '" + field + "' -- parse error" );
            }
            if ( value < 0 ) {
                return withError_minus1( what + " time field of '" + field + "' -- negative" );
            }
            if ( value > max ) {
                return withError_minus1( what + " time field of '" + field + "' -- exceeded max value of " + max );
            }
            return value;
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
                value = parseValue( fraction, "Second", what, 999 );
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
            return parseValue( value, what, "offset", max );
        }

        private int checkPostZ( int zAt, String value ) {
            String postZ = value.substring( zAt ).trim();
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

        private int parseValue( String strValue, String what, String type, int max ) {
            strValue = strValue.trim();
            int intValue;
            try {
                intValue = Integer.parseInt( strValue );
            }
            catch ( NumberFormatException e ) {
                return withError_minus1( what + " " + type + " of '" + strValue + "' -- parse error" );
            }
            if ( intValue < 0 ) {
                return withError_minus1( what + " " + type + " of '" + strValue + "' -- negative" );
            }
            if ( intValue > max ) {
                return withError_minus1( what + " " + type + " of '" + strValue + "' -- exceeded max value of " + max );
            }
            return intValue;
        }
    }
}
