// Copyright 2013 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
// www.source-code.biz, www.inventec.ch/chdh
//
// This module is multi-licensed and may be used under the terms
// of any of the following licenses:
//
//  EPL, Eclipse Public License, V1.0 or later, http://www.eclipse.org/legal
//  LGPL, GNU Lesser General Public License, V2.1 or later, http://www.gnu.org/licenses/lgpl.html
//
// Please contact the author if you need another license.
// This module is provided "as is", without warranties of any kind.

package com.wisense.cadal;

/**
* An IIR (infinite impulse response) filter.
*
* <p>
* Filter schema: <a href="http://commons.wikimedia.org/wiki/File:IIR_Filter_Direct_Form_1.svg">Wikipedia</a>
*
* <p>
* Formula:
* <pre>
*    y[i] = x[i] * b[0]  +  x[i-1] * b[1]  +  x[i-2] * b[2]  +  ...
*                        -  y[i-1] * a[1]  -  y[i-2] * a[2]  -  ...
* </pre>
* (x = input, y = output, a and b = filter coefficients, a[0] must be 1)
*/
public class IirFilterArray {

private int                  n1;                           // size of input delay line
private int                  n2;                           // size of output delay line
private double[]             a;                            // A coefficients, applied to output values (negative)
private double[]             b;                            // B coefficients, applied to input values

private double[]             buf1;                         // input signal delay line (ring buffer)
private double[]             buf2;                         // output signal delay line (ring buffer)
private int                  pos1;                         // current ring buffer position in buf1
private int                  pos2;                         // current ring buffer position in buf2

private int[] n1A;
private int[] n2A;
private double[][] buf1A;
private double[][] buf2A;
private int[] pos1A;
private int[] pos2A;

/**
* Creates an IIR filter.
*
* @param coeffs
*    The A and B coefficients. a[0] must be 1.
**/
//public IirFilter (double[] A, double[] B) {
//   a = A;
//   b = B;
//   if (a.length < 1 || b.length < 1 || a[0] != 1.0) {
//      throw new IllegalArgumentException("Invalid coefficients."); }
//   n1 = b.length - 1;
//   n2 = a.length - 1;
//   buf1 = new double[n1];
//   buf2 = new double[n2]; }

public IirFilterArray (double[] A, double[] B, int vectorElements){
	a=A;
	b=B;
	if(a.length<1||b.length<1||a[0]!=1.0){
		throw new IllegalArgumentException("Invalid coefficients.");}
	n1A=new int[vectorElements];
	n2A=new int[vectorElements];
	pos1A=new int[vectorElements];
	pos2A=new int[vectorElements];
	for (int i=0; i<vectorElements;i++){
		n1A[i]=b.length-1;
		n2A[i]=a.length-1;

	}
	buf1A=new double[vectorElements][n1A[0]];
	buf2A=new double[vectorElements][n2A[0]];
}

/**
* Processes one input signal value and returns the next output signal value.
*/
public double step (double inputValue) {
   double acc = b[0] * inputValue;
   for (int j = 1; j <= n1; j++) {
      int p = (pos1 + n1 - j) % n1;
      acc += b[j] * buf1[p]; }
   for (int j = 1; j <= n2; j++) {
      int p = (pos2 + n2 - j) % n2;
      acc -= a[j] * buf2[p]; }
   if (n1 > 0) {
      buf1[pos1] = inputValue;
      pos1 = (pos1 + 1) % n1; }
   if (n2 > 0) {
      buf2[pos2] = acc;
      pos2 = (pos2 + 1) % n2; }
   return acc; }



public double[] stepArray (float[] inputValue) {
	double[] acc=new double[inputValue.length];   
	for (int i = 0; i < inputValue.length; i++) {
		acc[i] = b[0] * inputValue[i];
		for (int j = 1; j <= n1A[i]; j++) {
			int p = (pos1A[i] + n1A[i] - j) % n1A[i];
			acc[i] += b[j] * buf1A[i][p];
		}
		for (int j = 1; j <= n2A[i]; j++) {
			int p = (pos2A[i] + n2A[i] - j) % n2A[i];
			acc[i] -= a[j] * buf2A[i][p];
		}
		if (n1A[i] > 0) {
			buf1A[i][pos1A[i]] = inputValue[i];
			pos1A[i] = (pos1A[i] + 1) % n1A[i];
		}
		if (n2A[i] > 0) {
			buf2A[i][pos2A[i]] = acc[i];
			pos2A[i] = (pos2A[i] + 1) % n2A[i];
		}
	} 
		return acc;
	
	   
}

	}
