/**
 * Created by gokul on 12/14/15.
 */
//package in.javadigest.encryption;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;


public class RSAEncryptor {

    /**
     * String to hold name of the encryption algorithm.
     */
    public static final String ALGORITHM = "RSA";
    public static final int KEY_LENGTH = 1024;

    public static long getSecureRandomNumber()
    {
        SecureRandom sRandom = new SecureRandom() ;

        byte[] seed1 = sRandom.generateSeed(8) ;
        sRandom.setSeed(seed1) ;
        long j = sRandom.nextLong();
        return j;

    }

    public static String getPublicKeyString(PublicKey publicK) {
        byte[] publicBytes = Base64.decodeBase64(String.valueOf(publicK));
        return new String(publicBytes);
    }

    public static PublicKey getPublicKeyFromString(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //key.indexOf("modulus:") + 9
        String str = key;
        str = str.substring(str.indexOf("modulus:") + 9);
        String modulus = str.substring(0, str.indexOf('\n'));
        String exponent = str.substring(str.indexOf(':') + 2);
        try {
            // for key modulus and exponent values as hex and decimal string respectively
            BigInteger keyInt = new BigInteger(modulus, 10); // hex base
            BigInteger exponentInt = new BigInteger(exponent, 10); // decimal base

            RSAPublicKeySpec keySpeck = new RSAPublicKeySpec(keyInt, exponentInt);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpeck);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Generate key which contains a pair of private and public key using KEY_LENGTH
     * bytes. Store the set of keys in Prvate.key and Public.key files.
     *
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static KeyPair generateKey() {
        try {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(KEY_LENGTH);
            final KeyPair key = keyGen.generateKeyPair();
            return key;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] getBytes(Object obj){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            return(bos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return new byte[0];
    }

    public static Object getObject(byte[] bytes){
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            return(in.readObject());
        } catch (ClassNotFoundException | IOException e) {
            //Bad practice do something useful here
            e.printStackTrace();
        } finally {
            try {
                bis.close();
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return null;
    }

    /**
     * Encrypt the plain text using public key.
     *
     * @param object
     *          : original plain text
     * @param key
     *          :The public key
     * @return Encrypted text
     * @throws Exception
     */
    public static String encrypt(Object object, PublicKey key) {
        byte[] cipherText = null;
        String returnString = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipherText = cipher.doFinal(getBytes(object));
            returnString = Base64.encodeBase64String(cipherText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnString;
    }

    public static String signContent(Object object, PrivateKey key) {
        byte[] cipherText = null;
        String returnString = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipherText = cipher.doFinal(getBytes(object));
            returnString = Base64.encodeBase64String(cipherText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnString;
    }


    /**
     * Decrypt text using private key.
     *
     * @param text
     *          :encrypted text
     * @param key
     *          :The private key
     * @return plain text
     * @throws Exception
     */
    public static Object decrypt(byte[] text, PrivateKey key) {
        byte[] dectyptedText = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(ALGORITHM);

            // decrypt the text using the private key
            cipher.init(Cipher.DECRYPT_MODE, key);
            dectyptedText = cipher.doFinal(text);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return getObject(dectyptedText);
    }

    public static Object authenticateSign(byte[] text, PublicKey key) {
        byte[] dectyptedText = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(ALGORITHM);

            // decrypt the text using the private key
            cipher.init(Cipher.DECRYPT_MODE, key);
            dectyptedText = cipher.doFinal(text);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return getObject(dectyptedText);
    }

}