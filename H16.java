// H16.java CS6025 Yizong Cheng March 2015
// Elliptic curve parameter checking
// Usage: java H16 curveSpecs

import java.math.*;
import java.io.*;
import java.util.*;
import java.math.BigInteger;
class Point{
  public BigInteger x; 
  public BigInteger y;
  static Point O = new Point(null, null);
  public Point(BigInteger xx, BigInteger yy){ x = xx; y = yy; }
  public String toString(){
    return this.equals(O) ? "O" :
    "(" + x.toString(16) + ",\n" + y.toString(16) + ")";
  }
}

public class H16{

  static BigInteger three = new BigInteger("3"); 
  static BigInteger alpha = new BigInteger("2");
  static BigInteger one = new BigInteger("1"); 
  static final int privateKeySize = 255;
  BigInteger p; // modulus
  Point G; // base point
  BigInteger a;  // curve parameter
  BigInteger b;  // curve parameter
  BigInteger n;  // order of G
  BigInteger privateKeyA;
  Point publicKeyA;
  BigInteger privateKeyB;
  Point publicKeyB;
  Random random = new Random();

void readCurveSpecs(String filename){ //provide filename
    Scanner in = null;
    try {
     in = new Scanner(new File(filename));
    } catch (FileNotFoundException e){
      System.err.println(filename + " not found");
      System.exit(1);
    }
    p = new BigInteger(in.nextLine(), 16);
    n = new BigInteger(in.nextLine(), 16);
    a = new BigInteger(in.nextLine(), 16);
    b = new BigInteger(in.nextLine(), 16 );
    G = new Point(new BigInteger(in.nextLine(), 16), new BigInteger(in.nextLine(), 16));
    in.close();
}

Point add(Point P1, Point P2){
   if (P1.equals(Point.O)) return P2;
   if (P2.equals(Point.O)) return P1;
   if (P1.x.equals(P2.x)) if (P1.y.equals(P2.y)) return selfAdd(P1);
                          else return Point.O;
   BigInteger t1 = P1.x.subtract(P2.x).mod(p);
   BigInteger t2 = P1.y.subtract(P2.y).mod(p);
   BigInteger k = t2.multiply(t1.modInverse(p)).mod(p); // slope
   t1 = k.multiply(k).subtract(P1.x).subtract(P2.x).mod(p); // x3
   t2 = P1.x.subtract(t1).multiply(k).subtract(P1.y).mod(p); // y3
   return new Point(t1,t2);
}   

Point selfAdd(Point P){
     if (P.equals(Point.O)) return Point.O; // O+O=O
     BigInteger t1 = P.y.add(P.y).mod(p);  // 2y
     BigInteger t2 = P.x.multiply(P.x).mod(p).multiply(three).add(a).mod(p); // 3xx+a
     BigInteger k = t2.multiply(t1.modInverse(p)).mod(p); // slope or tangent
     t1 = k.multiply(k).subtract(P.x).subtract(P.x).mod(p); // x3 = kk-x-x
     t2 = P.x.subtract(t1).multiply(k).subtract(P.y).mod(p); // y3 = k(x-x3)-y
     return new Point(t1,t2);
}

Point multiply(Point P, BigInteger n){
     if(n.equals(BigInteger.ZERO)) return Point.O;  
     int len = n.bitLength();  // position preceding the most significant bit 1
     Point product = P;
     for(int i = len - 2; i >= 0; i--){
        product = selfAdd(product); 
        if (n.testBit(i)) product = add(product, P);
     }
     return product;
  }

 void checkParameters(){
   BigInteger lhs = G.y.multiply(G.y).mod(p);
   BigInteger rhs = G.x.modPow(three, p).add(G.x.multiply(a).mod(p)).add(b).mod(p);
   System.out.println(lhs.toString(16));
   System.out.println(rhs.toString(16));  // These two lines should be the same
   Point power = multiply(G, n);
   System.out.println(power);  // This should be O
   System.out.println("");
 }

 void generateKeys(){
    privateKeyA = new BigInteger(privateKeySize, random);
    publicKeyA = multiply(G, privateKeyA);
    privateKeyB = new BigInteger(privateKeySize, random);
    publicKeyB = multiply(G, privateKeyB);
 }

 void sharedSecret(){ //Diffie-Helman analogue
    Point KA = multiply(publicKeyB, privateKeyA);  // secret computed by A
    Point KB = multiply(publicKeyA, privateKeyB); // secret computed by B
    System.out.println("Shared Keys: ");
    System.out.println(KA);
    System.out.println(KB);
 }  

 void encryptionForB(){
   BigInteger message = new BigInteger(privateKeySize, random);
   System.out.println(message.toString(16));
   BigInteger k = new BigInteger(privateKeySize, random);
   Point kG = multiply(G, k); // k times G
   Point kY = multiply(publicKeyB, k); // k times publicKeyB
   BigInteger mu = message.multiply(kY.x).mod(p);  // message times kY.x mod p
   System.out.println(kG); System.out.println(mu.toString(16));
   // (kG, mu) is the encrypted message
   Point kY2 = multiply(kG, privateKeyB);  // B computes kY as privateKeyB times kG
   BigInteger decodedMessage = kY2.x.modInverse(p).multiply(mu).mod(p); // kY2.x modinverse times mu mod p
   System.out.println("");
   System.out.println("Decoded Message: ");
   System.out.println(decodedMessage.toString(16));
   System.out.println("");
 }

 void ECDSA(){ //Look in book for this, ch. 13
   BigInteger message = new BigInteger(160, random); //works best for message for signature
   System.out.println(message.toString(16));
   BigInteger k = new BigInteger(privateKeySize, random);
   Point kG = multiply(G, k); // k times G
   BigInteger r = kG.x.mod(n); // r = kG.x mod n
   BigInteger s = k.modInverse(n).multiply(message.add(privateKeyB.multiply(r))).mod(n); // s = k's modInverse times (message plus privateKeyB times r) mod n
   // if r or s == 0 redo k
   // (r,s) is the signature for message (digest)
   BigInteger w = s.modInverse(n); // w is s's multiplicative inverse mod n
   BigInteger u1 = message.multiply(w); // u1 = message times w
   BigInteger u2 = r.multiply(w); // u2 = r times w
   Point X1 = multiply(G, u1);
   Point X2 = multiply(publicKeyB, u2);
   Point X = add(X1, X2);// X is u1 times G + u2 times publicKeyB
   System.out.println("");
   if (X.equals(Point.O)) System.out.println("Rejected signature"); // reject the signature
   else if (X.x.mod(n).equals(r)) {
     System.out.println("ECDSA\n" + X + "\n" + "Signature accepted");
   } // accept the signature 
   else System.out.println("Rejected signature");// reject the signature
 }
   
  //integer points x and y on the curve, binary operation to combine two points into one, end up with addition
//NSA backdoor in the scheme, cannot even see it 
public static void main(String[] args){
   H16 h16 = new H16();
   h16.readCurveSpecs(args[0]);
   h16.generateKeys();
   h16.sharedSecret();
   h16.encryptionForB();
   h16.ECDSA();
}
}

  