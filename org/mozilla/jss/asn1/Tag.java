/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.jss.asn1;

/**
 * Represents an ASN.1 Tag.  A tag consists of a class and a number.
 */
public class Tag {

    private long num;
    /**
     * @return The tag number.
     */
    public long getNum() {
        return num;
    }

    private Class tClass;
    /**
     * @return The tag class.
     */
    public Class getTagClass() {
        return tClass;
    }

    /**
     * A tag class.
     */
    public static final Class UNIVERSAL = Class.UNIVERSAL;
    /**
     * A tag class.
     */
    public static final Class APPLICATION = Class.APPLICATION;
    /**
     * A tag class.
     */
    public static final Class CONTEXT_SPECIFIC = Class.CONTEXT_SPECIFIC;
    /**
     * A tag class.
     */
    public static final Class PRIVATE = Class.PRIVATE;

    /**
     * The end-of-contents marker for indefinite length encoding.
     * It is encoded the same as an ASN.1 header whose tag is [UNIVERSAL 0].
     */
    public static final Tag END_OF_CONTENTS = new Tag( UNIVERSAL, 0 );

    /**
     * An alias for END_OF_CONTENTS.
     */
    public static final Tag EOC = END_OF_CONTENTS;

    /**
     * Creates a tag with the given class and number.
     * @param clazz The class of the tag.
     * @param num The tag number.
     */
    public Tag(Class clazz, long num) {
        tClass = clazz;
        this.num = num;
    }

    /**
     * Creates a CONTEXT-SPECIFIC tag with the given tag number.
     * @param num The tag number.
     */
    public Tag(long num) {
        this(Class.CONTEXT_SPECIFIC, num);
    }

    ///////////////////////////////////////////////////////////////////////
    // Tag Instances
    //
    // Since grabbing a context-specific tag is a very common operation,
    // let's make singletons of the most frequently used tags.
    ///////////////////////////////////////////////////////////////////////
    private static final int numTagInstances = 10;
    private static Tag tagInstances[] = new Tag[numTagInstances];
    static {
        for(int i=0; i < numTagInstances; i++) {
            tagInstances[i] = new Tag(i);
        }
    }

    /**
     * Returns an instance of a context-specific tag with the given number.
     * The returned instance may be singleton.  It is usually more efficient to
     * call this method than create your own context-specific tag.
     * @param num Number.
     * @return Tag.
     */
    public static Tag get(long num) {
        if( num >= 0 && num < numTagInstances ) {
            return tagInstances[(int)num];
        } else {
            return new Tag(num);
        }
    }

    public int hashCode() {
        return (tClass.toInt() * 131) + (int)num;
    }

    /**
     * Compares two tags for equality.  Tags are equal if they have
     * the same class and tag number.
     * @param obj Tag.
     * @return True if equal.
     */
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(! (obj instanceof Tag) ) {
            return false;
        }

        Tag t = (Tag) obj;
        if( num == t.num && tClass == t.tClass ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns a String representation of the tag. For example, a tag
     * whose class was UNIVERSAL and whose number was 16 would return
     * "UNIVERSAL 16".
     */
    public String toString() {
        return tClass+" "+num;
    }

    /**
     * An enumeration of the ASN.1 tag classes.
     */
    public static class Class {

        private Class() { }
        private Class(int enc, String name) {
            encoding = enc;
            this.name = name;
        }
        private int encoding;
        private String name;

        public static final Class UNIVERSAL = new Class(0, "UNIVERSAL");
        public static final Class APPLICATION = new Class(1, "APPLICATION");
        public static final Class CONTEXT_SPECIFIC =
            new Class(2, "CONTEXT-SPECIFIC");
        public static final Class PRIVATE = new Class(3, "PRIVATE");

        public int toInt() {
            return encoding;
        }

        public String toString() {
            return name;
        }

        /**
         * @param i Tag encoding.
         * @return Tag class.
         * @exception InvalidBERException If the given int does not correspond
         *      to any tag class.
         */
        public static Class fromInt(int i) throws InvalidBERException {
            if( i == 0 ) {
                return UNIVERSAL;
            } else if(i == 1) {
                return APPLICATION;
            } else if(i == 2) {
                return CONTEXT_SPECIFIC;
            } else if(i == 3) {
                return PRIVATE;
            } else {
                throw new InvalidBERException("Invalid tag class: " + i);
            }
        }
    }
}
