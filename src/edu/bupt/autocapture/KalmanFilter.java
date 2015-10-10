package edu.bupt.autocapture;

public class KalmanFilter {
	private double[] ry = new double[10];
	public int n = -1;
	private double result = 0;

	public void inputData(double data) {
		for (int i = 9; i > 0; i--) {
			ry[i] = ry[i - 1];
		}
		ry[0] = data;
	}

	public void returnry() {
		for (int i = 0; i < 10; i++) {
			System.out.print(ry[i] + " ");
		}
		System.out.println();
	}

	public void init() {
		for (int i = 0; i < 10; i++) {
			ry[i] = 0;
		}
		n = ry.length;
	}

	// 卡尔曼滤波，返回一个预测的double值
	public double filter() {
		if (n == -1) {
			return 0.0;
		}// number of data values
		double[] rt = new double[n];// t ordinates
		for (int i = 0; i < n; i++) {
			rt[i] = i + 1;
		}
		// rt中存放1到186的序列
		int m = 5;// derivative in penalty
		// m=2 produces a cubic smoothing spline

		double lam = 3;// smoothing parameter value
		// For this data and this choice of lambda, the same result will
		// be obtained using spar=1 in the R smooth.spline function with
		// all.knots=T

		ArrayToVector y = new ArrayToVector(ry);// convert array to vector
		ArrayToVector tau = new ArrayToVector(rt);// convert array to vector
		// y为数据向量。tau为序列向量,代表时间坐标
		// y.printVector();
		// System.out.println("---------------------------------");
		// tau.printVector();

		// ss objects for processing y
		SmoothingSpline[] s = new SmoothingSpline[n];
		// ss objects for processing polynomial matrix
		SmoothingSpline[][] sT = new SmoothingSpline[n][m];
		// polynomial matrix
		Matrix T = new Matrix(tau, m);

		// initialize forward recusion for y
		s[0] = new SmoothingSpline(1, tau, m, lam);
		s[0].forward(new Matrix(m, 0), new ArrayToVector(m, 0), new ArrayToVector(y.value(0)));
		// initialize forward recusion for T
		for (int j = 0; j < m; j++) {
			sT[0][j] = new SmoothingSpline(1, tau, m, lam);
			sT[0][j].forward(new Matrix(m, 0), new ArrayToVector(m, 0),
					new ArrayToVector(T.value(0, j)));
		}
		// forward recursion
		for (int i = 1; i < n; i++) {
			s[i] = new SmoothingSpline(i + 1, tau, m, lam);
			s[i].forward(s[i - 1].GetS(), s[i - 1].Getx(),
					new ArrayToVector(y.value(i)));

			for (int j = 0; j < m; j++) {
				sT[i][j] = new SmoothingSpline(i + 1, tau, m, lam);
				sT[i][j].forward(sT[i - 1][j].GetS(), sT[i - 1][j].Getx(),
						new ArrayToVector(T.value(i, j)));
			}

		}
		// define xn and Sn for s[n-1] for use in subsequent recusions
		s[n - 1].Setxntox();
		s[n - 1].SetSntoS();
		// initialize backward recusion for y
		s[n - 2].smooth(new ArrayToVector(m, 0), new Matrix(m, 0),
				s[n - 1].GetHTRinv(), s[n - 1].Geteps());
		// initialize backward recusion for T
		for (int j = 0; j < m; j++) {
			sT[n - 1][j].Setxntox();
			sT[n - 2][j].smooth(new ArrayToVector(m, 0), new Matrix(m, 0),
					sT[n - 1][j].GetHTRinv(), sT[n - 1][j].Geteps());
		}
		// backward recursion
		for (int i = n - 3; i >= 0; i--) {
			s[i].smooth(s[i + 1].Geta(), s[i + 1].GetA(), s[i + 1].GetHTRinv(),
					s[i + 1].Geteps());
			for (int j = 0; j < m; j++) {
				sT[i][j].smooth(sT[i + 1][j].Geta(), sT[i + 1][j].GetA(),
						sT[i + 1][j].GetHTRinv(), sT[i + 1][j].Geteps());
			}
		}

		Matrix Ttil = new Matrix(n, m, 0);
		ArrayToVector fit = new ArrayToVector(n, 0);
		for (int i = 0; i < n; i++) {
			fit.setValue(i, s[i].Getxn().value(0));
			for (int j = 0; j < m; j++)
				Ttil.setValue(i, j, T.value(i, j) - sT[i][j].Getxn().value(0));
		}
		ArrayToVector g_0 = Ttil.trans().times(y);
		Matrix V = Ttil.trans().times(T).chol(new Matrix(m, 1));
		fit = fit.plus(Ttil.times(V.times(g_0)));
		result = fit.value(0);
		return result;
	}
}
