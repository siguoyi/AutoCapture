package edu.bupt.autocapture;

class ArrayToVector{
    //number of rows
    private int nr;
    //a double array to store the data
    private double[] X;
    
    //constructor: array to a vector
    ArrayToVector(double [] a){
	nr=a.length;
	X = new double [nr];
	for(int i=0;i<nr;i++)
	    X[i]=a[i];
    }

    //copy constructor: vector to a vector
    ArrayToVector(ArrayToVector a){
	nr=a.GetNrow();
	X = new double [nr];
	for(int i=0;i<nr;i++)
	    X[i]=a.value(i);
    }


    //overloaded constructor: constant vector 
    ArrayToVector(int Nr, double a){
	nr=Nr;
	X=new double[nr];
	for(int i=0;i<nr;i++)
	    X[i]=a;
    }

    //overloaded constructor: matrix as a vector 
    ArrayToVector(Matrix A){
	if(A.GetNcol()!=1){
	    System.out.println("A is not a vector");
	    System.exit(1);
	}
	nr=A.GetNrow();
	X=new double[nr];
	for(int i=0;i<nr;i++)
	    X[i]=A.value(i,0);
    }


    //overloaded constructor: extract column of matrix as a vector 
    ArrayToVector(Matrix A, int j){
	nr=A.GetNrow();
	X=new double[nr];
	for(int i=0;i<nr;i++)
	    X[i]=A.value(i,j);
    }

    //overloaded constructor: scalar as a vector 
    ArrayToVector(double a){
	nr=1;
	X=new double[1];
	X[0]=a;
    }


    //
    //
    //basic matrix operations
    //
    //
 
    //vector addition
    ArrayToVector plus(ArrayToVector Y){
	if(nr != Y.GetNrow()){
	    System.out.println("Dimensions not compatible");
	    System.exit(1);
	}
	ArrayToVector XplusY=new ArrayToVector(nr, 0);
	for(int i=0;i<nr;i++)
	    XplusY.setValue(i,X[i]+Y.value(i));

	return XplusY;
    }


   //vector subtraction
    ArrayToVector minus(ArrayToVector Y){
	if(nr != Y.GetNrow()){
	    System.out.println("Dimensions not compatible");
	    System.exit(1);
	}
	ArrayToVector XminusY=new ArrayToVector(nr,0);
	for(int i=0;i<nr;i++)
	    XminusY.setValue(i,X[i]-Y.value(i));

	return XminusY;
    }


    //vector multiplication

    //inner product
    double inner(ArrayToVector Y){
	if(nr != Y.GetNrow()){
	    System.out.println("Dimensions not compatible");
	    System.exit(1);
	}
	double prod=0;
	for(int i=0;i<nr;i++)
	    prod+=X[i]*Y.value(i);

	return prod;
    }

    //outer product
    Matrix outer(ArrayToVector Y){
	double temp;
	Matrix XtimesY=new Matrix(nr,Y.GetNrow(),0.);
	for(int i=0;i<nr;i++)
	    for(int j=0;j<Y.GetNrow();j++)
		XtimesY.setValue(i,j,X[i]*Y.value(j));
	
	return XtimesY;
	}


    //scalar multiplication
    ArrayToVector times(double Y){
	ArrayToVector XtimesY=new ArrayToVector(nr,0);
	for(int i=0;i<nr;i++)
	    XtimesY.setValue(i,X[i]*Y);

	return XtimesY;
    }
    
    //
    //access to a member elements
    //

    double value(int r){
	return X[r];
    }

    int GetNrow(){
	return nr;
    }

    void setValue(int r, double a){
	X[r]=a;
    }
    //
    //utilities
    //
    double[] getRetValues(){
    	
    	return X;
    	
    }
    
    void printVector(){
	for(int i=0; i<nr ;i++){
	    System.out.print(X[i]+" ");
	}
	System.out.println(" ");
    }


}
