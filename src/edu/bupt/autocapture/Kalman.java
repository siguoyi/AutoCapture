package edu.bupt.autocapture;

abstract class Kalman {
	int t;
	Matrix Sm1, S, R, HTRinv, M, Sn, A;
	ArrayToVector eps, x, xn, a;

	// Algorithm 4.3
	void forward(Matrix Sm1m1, ArrayToVector xm1m1, ArrayToVector y) {
		Sm1 = F(t - 1).times(Sm1m1.times(F(t - 1).trans())).plus(Q(t - 1));
		R = H(t).times(Sm1.times(H(t).trans())).plus(W(t));
		HTRinv = R.chol(H(t)).trans();
		Matrix temp1 = Sm1.times(HTRinv.times(H(t)));// temporary storage
		S = Sm1.minus(temp1.times(Sm1));
		M = F(t).minus(F(t).times(temp1));
		ArrayToVector temp2 = F(t - 1).times(xm1m1);// temporary storage
		eps = y.minus(H(t).times(temp2));
		x = Sm1.times(HTRinv.times(eps)).plus(temp2);
	}

	abstract Matrix Q(int i);

	abstract Matrix F(int i);

	abstract Matrix H(int i);

	abstract Matrix W(int i);

	// A variant of Algorithm 5.1 that initializes a and A as 0 with updating
	// at the beginning of each step rather than the end. Note the ``typo'' in
	// the
	// text where M(t-1) is not defined for the last step with t=1. Since a and
	// A
	// do not require updating in this case, the algorithm works as written
	// provided
	// the last two lines of the for loop are implemented conditionally on t>1.
	void smooth(ArrayToVector aIn, Matrix AIn, Matrix HTRinvIn, ArrayToVector epsIn) {
		a = M.trans().times(HTRinvIn.times(epsIn)).plus(M.trans().times(aIn));
		A = M.trans().times(HTRinvIn.times(H(t + 1).times(M)))
				.plus(M.trans().times(AIn.times(M)));
		xn = x.plus(Sm1.times(a));
		Sn = S.minus(Sm1.times(A.times(Sm1)));
	}

	// access to members

	int Gett() {
		return t;
	}

	Matrix GetSm1() {
		return Sm1;
	}

	Matrix GetS() {
		return S;
	}

	Matrix GetR() {
		return R;
	}

	Matrix GetHTRinv() {
		return HTRinv;
	}

	Matrix GetM() {
		return M;
	}

	Matrix GetSn() {
		return Sn;
	}

	Matrix GetA() {
		return A;
	}

	ArrayToVector Getx() {
		return x;
	}

	ArrayToVector Geteps() {
		return eps;
	}

	ArrayToVector Getxn() {
		return xn;
	}

	// useful for setting xn=x for last step in forward
	// (first step in backward) recursion
	void Setxntox() {
		this.xn = new ArrayToVector(x);
	}

	// useful for setting Sn=S for last step in forward
	// (first step in backward) recursion
	void SetSntoS() {
		this.Sn = new Matrix(S);
	}

	ArrayToVector Geta() {
		return a;
	}

	// utilities

	void printQ(int i) {
		System.out.println("Q(" + i + ")");
		Q(i).printMatrix();
	}

	void printF(int i) {
		System.out.println("F(" + i + ")");
		F(i).printMatrix();
	}

	void printH(int i) {
		System.out.println("H(" + i + ")");
		H(i).printMatrix();
	}

	void printW(int i) {
		System.out.println("W(" + i + ")");
		W(i).printMatrix();
	}

	void forwardPrint() {
		System.out.println("eps(" + t + ")");
		eps.printVector();

		System.out.println("x(" + t + "|" + t + ")");
		x.printVector();

		System.out.println("S(" + t + "|" + t + ")");
		S.printMatrix();
	}

	void backPrint() {
		System.out.println("x(" + t + "|n)");
		xn.printVector();
		System.out.println("S(" + t + "|n)");
		Sn.printMatrix();
	}

}
