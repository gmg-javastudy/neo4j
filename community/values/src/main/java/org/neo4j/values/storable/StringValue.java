/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.values.storable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.hashing.HashFunction;
import org.neo4j.internal.helpers.collection.Pair;
import org.neo4j.values.AnyValue;
import org.neo4j.values.ValueMapper;
import org.neo4j.values.virtual.ListValue;
import org.neo4j.values.virtual.VirtualValues;

import static java.lang.String.format;

public abstract class StringValue extends TextValue
{
    abstract String value();

    @Override
    public boolean equals( Value value )
    {
        return value.equals( value() );
    }

    @Override
    public boolean equals( char x )
    {
        return value().length() == 1 && value().charAt( 0 ) == x;
    }

    @Override
    public boolean equals( String x )
    {
        return value().equals( x );
    }

    @Override
    public <E extends Exception> void writeTo( ValueWriter<E> writer ) throws E
    {
        writer.writeString( value() );
    }

    @Override
    public TextValue toLower()
    {
        return new StringWrappingStringValue( value().toLowerCase() );
    }

    @Override
    public TextValue toUpper()
    {
        return new StringWrappingStringValue( value().toUpperCase() );
    }

    @Override
    public ListValue split( String separator )
    {
        assert separator != null;
        String asString = value();
        //Cypher has different semantics for the case where the separator
        //is exactly the value, in cypher we expect two empty arrays
        //where as java returns an empty array
        if ( separator.equals( asString ) )
        {
            return EMPTY_SPLIT;
        }
        else if ( separator.isEmpty() )
        {
            return VirtualValues.fromArray( Values.charArray( asString.toCharArray() ) );
        }

        List<AnyValue> split = splitNonRegex( asString, separator );
        return VirtualValues.fromList( split );
    }

    @Override
    public ListValue split( List<String> separators )
    {
        assert separators != null;
        String asString = value();
        //Cypher has different semantics for the case where the separator
        //is exactly the value, in cypher we expect two empty arrays
        //where as java returns an empty array
        if ( separators.stream().anyMatch( sep -> sep.equals( asString ) ) )
        {
            return EMPTY_SPLIT;
        }
        else if ( separators.stream().anyMatch( String::isEmpty ) )
        {
            String reduced = asString;
            for ( var sep : separators )
            {
                if ( sep.isEmpty() )
                {
                    continue;
                }
                reduced = reduced.replace( sep, "" );
            }
            return VirtualValues.fromArray( Values.charArray( reduced.toCharArray() ) );
        }

        List<AnyValue> split = splitNonRegex( asString, separators );
        return VirtualValues.fromList( split );
    }

    /**
     * Splits a string based on a single delimiter string
     *
     * @param input String to be split
     * @param delim delimiter, must not be not empty
     * @return the split string as a List of TextValues
     */
    private static List<AnyValue> splitNonRegex( String input, String delim )
    {
        List<AnyValue> substrings = new ArrayList<>();
        int offset = 0;
        int index;

        do
        {
            index = input.indexOf( delim, offset );
            offset = updateSubstringsAndOffset( substrings, offset, input, index, delim );
        }
        while ( index != -1 );
        return substrings;
    }

    /**
     * Splits a string with multiple separator strings
     *
     * @param input String to be split
     * @param delims delimiters, must not be not empty
     * @return the split string as a List of TextValues
     */
    private static List<AnyValue> splitNonRegex( String input, List<String> delims )
    {
        List<AnyValue> substrings = new ArrayList<>();
        int offset = 0;
        Pair<Integer,String> nextSubstring;

        do
        {
            nextSubstring = firstIndexOf( input, offset, delims );
            offset = updateSubstringsAndOffset( substrings, offset, input, nextSubstring.first(), nextSubstring.other() );
        }
        while ( nextSubstring.first() != -1 );
        return substrings;
    }

    /**
     * Make decisions based on whether the specified delimiter had been found or not.
     * If found, add a new substring to the collection, and return a new offset after the delimiter.
     */
    private static int updateSubstringsAndOffset( List<AnyValue> substrings, int offset, String input, int index, String delim )
    {
        if ( index == -1 )
        {
            String substring = input.substring( offset );
            substrings.add( Values.stringValue( substring ) );
        }
        else
        {
            String substring = input.substring( offset, index );
            substrings.add( Values.stringValue( substring ) );
            offset = index + delim.length();
        }
        return offset;
    }

    /**
     * Search the input string, starting at the specified offset, for any of the the specified delimiter strings.
     * The first delimiter found will be returned with it's starting index position.
     */
    private static Pair<Integer,String> firstIndexOf( String input, int offset, List<String> delims )
    {
        int firstIndex = -1;
        String first = null;
        for ( var delim : delims )
        {
            int index = input.indexOf( delim, offset );
            if ( index != -1 )
            {
                if ( first == null || index < firstIndex )
                {
                    first = delim;
                    firstIndex = index;
                }
            }
        }
        return Pair.of( firstIndex, first );
    }

    @Override
    public TextValue replace( String find, String replace )
    {
        assert find != null;
        assert replace != null;

        return Values.stringValue( value().replace( find, replace ) );
    }

    @Override
    public Object asObjectCopy()
    {
        return value();
    }

    @Override
    public String toString()
    {
        return format( "%s(\"%s\")", getTypeName(), value() );
    }

    @Override
    public String getTypeName()
    {
        return "String";
    }

    @Override
    public String stringValue()
    {
        return value();
    }

    @Override
    public String prettyPrint()
    {
        return format( "'%s'", value() );
    }

    @Override
    public <T> T map( ValueMapper<T> mapper )
    {
        return mapper.mapString( this );
    }

    //NOTE: this doesn't respect code point order for code points that doesn't fit 16bits
    @Override
    public int compareTo( TextValue other )
    {
        String thisString = value();
        String thatString = other.stringValue();
        return thisString.compareTo( thatString );
    }

    @Override
    public boolean isSameValueTypeAs( Value value )
    {
        return value instanceof StringValue;
    }

    static final TextValue EMPTY = new StringValue()
    {
        @Override
        protected int computeHash()
        {
            return 0;
        }

        @Override
        public long updateHash( HashFunction hashFunction, long hash )
        {
            return hashFunction.update( hash, 0 ); // Mix in our length; a single zero.
        }

        @Override
        public int length()
        {
            return 0;
        }

        @Override
        public TextValue substring( int start, int end )
        {
            return this;
        }

        @Override
        public TextValue trim()
        {
            return this;
        }

        @Override
        public TextValue ltrim()
        {
            return this;
        }

        @Override
        public TextValue rtrim()
        {
            return this;
        }

        @Override
        public TextValue reverse()
        {
            return this;
        }

        @Override
        public TextValue plus( TextValue other )
        {
            return other;
        }

        @Override
        public boolean startsWith( TextValue other )
        {
            return other.length() == 0;
        }

        @Override
        public boolean endsWith( TextValue other )
        {
            return other.length() == 0;
        }

        @Override
        public boolean contains( TextValue other )
        {
            return other.length() == 0;
        }

        @Override
        public TextValue toLower()
        {
            return this;
        }

        @Override
        public TextValue toUpper()
        {
            return this;
        }

        @Override
        public TextValue replace( String find, String replace )
        {
            if ( find.isEmpty() )
            {
                return Values.stringValue( replace );
            }
            else
            {
                return this;
            }
        }

        @Override
        public int compareTo( TextValue other )
        {
            return -other.length();
        }

        @Override
        Matcher matcher( Pattern pattern )
        {
            return pattern.matcher( "" );
        }

        @Override
        String value()
        {
            return "";
        }

        @Override
        protected long estimatedPayloadSize()
        {
            return 0;
        }

        @Override
        public long estimatedHeapUsage()
        {
            return 0;
        }
    };
}

