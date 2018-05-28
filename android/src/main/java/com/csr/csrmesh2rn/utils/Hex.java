package com.csr.csrmesh2rn.utils;

import java.io.PrintStream;

public class Hex
{
  private static final char[] a = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
  private static final char[] b = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

  public static char[] encodeHex(byte[] paramArrayOfByte)
  {
    return encodeHex(paramArrayOfByte, true);
  }

  public static char[] encodeHex(byte[] paramArrayOfByte, boolean paramBoolean)
  {
    return a(paramArrayOfByte, paramBoolean ? a : b);
  }

  protected static char[] a(byte[] paramArrayOfByte, char[] paramArrayOfChar)
  {
    if (paramArrayOfByte == null) {
      return null;
    }
    int i = paramArrayOfByte.length;
    char[] arrayOfChar = new char[i << 1];
    int j = 0;
    int k = 0;
    while (j < i)
    {
      arrayOfChar[(k++)] = paramArrayOfChar[((0xF0 & paramArrayOfByte[j]) >>> 4)];
      arrayOfChar[(k++)] = paramArrayOfChar[(0xF & paramArrayOfByte[j])];
      j++;
    }
    return arrayOfChar;
  }

  public static String encodeHexStr(byte[] paramArrayOfByte, int paramInt)
  {
    if (paramArrayOfByte != null)
    {
      if (paramInt > paramArrayOfByte.length) {
        paramInt = paramArrayOfByte.length;
      }
      if (paramInt > 0)
      {
        char[] arrayOfChar1 = b;
        char[] arrayOfChar2 = new char[paramInt << 1];
        int i = 0;
        int j = 0;
        while (i < paramInt)
        {
          arrayOfChar2[(j++)] = arrayOfChar1[((0xF0 & paramArrayOfByte[i]) >>> 4)];
          arrayOfChar2[(j++)] = arrayOfChar1[(0xF & paramArrayOfByte[i])];
          i++;
        }
        return new String(arrayOfChar2);
      }
    }
    return null;
  }

  public static String encodeHexStr(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
  {
    if (paramArrayOfByte != null)
    {
      if (paramInt2 > paramArrayOfByte.length - paramInt1) {
        paramInt2 = paramArrayOfByte.length - paramInt1;
      }
      if (paramInt2 > 0)
      {
        char[] arrayOfChar1 = b;
        char[] arrayOfChar2 = new char[paramInt2 << 1];
        int i = 0;
        int j = 0;
        while (i < paramInt2)
        {
          arrayOfChar2[(j++)] = arrayOfChar1[((0xF0 & paramArrayOfByte[(i + paramInt1)]) >>> 4)];
          arrayOfChar2[(j++)] = arrayOfChar1[(0xF & paramArrayOfByte[(i + paramInt1)])];
          i++;
        }
        return new String(arrayOfChar2);
      }
    }
    return null;
  }

  public static String encodeHexStr(byte[] paramArrayOfByte)
  {
    return encodeHexStr(paramArrayOfByte, true);
  }

  public static String encodeHexStr(byte[] paramArrayOfByte, boolean paramBoolean)
  {
    return b(paramArrayOfByte, paramBoolean ? a : b);
  }

  protected static String b(byte[] paramArrayOfByte, char[] paramArrayOfChar)
  {
    return paramArrayOfByte == null ? null : new String(a(paramArrayOfByte, paramArrayOfChar));
  }

  public static byte[] decodeHex(char[] paramArrayOfChar)
  {
    if (paramArrayOfChar == null) {
      return null;
    }
    int i = paramArrayOfChar.length;
    if ((i & 0x1) != 0) {
      throw new RuntimeException("Odd number of characters.");
    }
    byte[] arrayOfByte = new byte[i >> 1];
    int j = 0;
    int k = 0;
    while (k < i)
    {
      int m = a(paramArrayOfChar[k], k) << 4;
      k++;
      m |= a(paramArrayOfChar[k], k);
      k++;
      arrayOfByte[j] = ((byte)(m & 0xFF));
      j++;
    }
    return arrayOfByte;
  }

  protected static int a(char paramChar, int paramInt)
  {
    int i = Character.digit(paramChar, 16);
    if (i == -1) {
      throw new RuntimeException("Illegal hexadecimal character " + paramChar + " at index " + paramInt);
    }
    return i;
  }

  public static void main(String[] paramArrayOfString)
  {
    String str1 = "待转换字符串";
    String str2 = encodeHexStr(str1.getBytes());
    String str3 = new String(decodeHex(str2.toCharArray()));
    System.out.println("转换前：" + str1);
    System.out.println("转换后：" + str2);
    System.out.println("还原后：" + str3);
  }
}
