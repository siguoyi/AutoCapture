package edu.bupt.autocapture;

//This class provides specific examples of the abstract
//methods in class Kalman that arise from smoothing
//spline considerations.
//The x process corresponds to m-fold integrated Brownian 
//motion.



class SmoothingSpline extends Kalman{
  //members
  ArrayToVector tau;//vector of "time" ordinates
  int m;//order of derivative in penalty
  double lam;//smoothing parameter

  //constructor
  SmoothingSpline(int T, ArrayToVector Tau, int M, double Lambda){
	t=T;
	tau=new ArrayToVector(Tau);
	m=M;
	lam=Lambda;
  }

  Matrix Q(int k){
	double den=0, num=0;
	double delta=0;

	if(k==0)delta=tau.value(0);
	if(k!=0)delta=tau.value(k)-tau.value(k-1);

	Matrix A = new Matrix(m,0);
	for(int i=0;i<m;i++)
	    for(int j=0;j<m;j++){
		den=fac(m-1-i)*fac(m-1-j);
		den*=(double)(2*m-1-i-j);
		num=Math.pow(delta,2*m-1-i-j)/lam;
		A.setValue(i,j,num/den);
	    }
	
	return A;
  }
	
 
  Matrix F(int k){
	Matrix A;
	double delta=0;
	if(k==tau.GetNrow()){
	    A=new Matrix(m,0);
	}

	else{
	    if(k==0)delta=tau.value(0);
	
	    if(k!=0)delta=tau.value(k)-tau.value(k-1);

	    A=new Matrix(m,1);//m by m identity
	    if(m!=1){
		for(int i=0;i<(m-1);i++)
		    for(int j=i+1;j<m;j++){
			A.setValue(i,j,Math.pow(delta,j-i)/fac(j-i));
		    }
	    }
	}
	return A;
  }


  Matrix H(int k){
	Matrix A=new Matrix(1,m,0);
	A.setValue(0,0,1);
	return A;
  }

  Matrix W(int k){
	Matrix A= new Matrix(1,1,1);
	return A;
  }

  static double fac(int k){
	double f=1;
	if(k==0)f=1;
	if(k!=0){
	    for(int i=1;i<=k;i++)
		f=f*(double)i;
	}
	return f;
  }

  ArrayToVector Gettau(){
	return tau;
  }

  int Getm(){
	return m;
  }

}
