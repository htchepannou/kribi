package io.tchepannou.kribi.aws.support;

public class AwsSupport {
    public static String nomalizeName(final String name){
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            final char ch = name.charAt(i);
            if (ch == '-' || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                sb.append(ch);
            } else {
                sb.append('-');
            }
        }
        return sb.toString().toLowerCase();
    }
}
