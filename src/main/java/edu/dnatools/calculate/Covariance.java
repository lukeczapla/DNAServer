package edu.dnatools.calculate;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.eigen.Eigen;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.inverse.InvertMatrix;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by luke on 7/9/17.
 */
public class Covariance {

    private static final Logger log = LoggerFactory.getLogger(Covariance.class);

    public static INDArray mpow(INDArray in, int n, boolean inplace) {
        assert in.rows() == in.columns();
        if (n == 0) {
            if (inplace) return in.assign(Nd4j.eye(in.rows()));
                else return Nd4j.eye(in.rows());
        }
        INDArray temp;
        if (n < 0) {
            temp = InvertMatrix.invert(in, false);
            n = -n;
        } else temp = in.dup();
        INDArray result = temp.dup();
        for (int i = 1; i < n; i++) {
            result.mmuli(temp);
        }
        if (inplace) in.assign(result);
        return result;
    }

    public static INDArray pca(INDArray in, double variance) {
        INDArray[] covmean = covarianceMatrix(in);
        INDArray[] pce = principalComponents(covmean[0]);
        INDArray vars = Transforms.pow(Transforms.sqrt(pce[1], false), -1, false);
        double res = vars.sumNumber().doubleValue();
        double total = 0.0;
        int ndims = 0;
        for (int i = 0; i < vars.columns(); i++) {
            ndims++;
            total += vars.getDouble(i);
            if (total/res > variance) break;
        }
        INDArray result = Nd4j.create(in.columns(), ndims);
        for (int i = 0; i < ndims; i++)
            result.putColumn(i, pce[0].getColumn(i));
        return result;
    }

    /**
     * Returns the covariance matrix of a dataset
     *
     * @param in A matrix of vectors of length N on each row
     * @return an N x N covariance matrix is element 0, and average values is element 1.
     */
    public static INDArray[] covarianceMatrix(INDArray in) {
        int dlength = in.size(0);
        int vlength = in.size(1);

        INDArray sum = Nd4j.create(vlength);
        INDArray product = Nd4j.create(vlength, vlength);

        for (int i = 0; i < vlength; i++)
            sum.getColumn(i).assign(in.getColumn(i).sumNumber().doubleValue()/dlength);

        for (int i = 0; i < dlength; i++) {
            INDArray dx1 = in.getRow(i).sub(sum);
            product.addi(dx1.reshape(vlength,1).mmul(dx1.reshape(1,vlength)));
        }
        product.divi(dlength);
        return new INDArray[] {product, sum};
    }


    /**
     * Calculates the principal component vectors and their eigenvalues for the covariance matrix
     * @param cov The covariance matrix (calculated with the covarianceMatrix(in) method)
     * @return An array of INDArray.  The principal component vectors in decreasing flexibility is element 0
     *      and the eigenvalues is element 1
     */
    public static INDArray[] principalComponents(INDArray cov) {
        assert cov.rows() == cov.columns();
        INDArray[] result = new INDArray[2];
        result[0] = Nd4j.eye(cov.rows());
        result[1] = Eigen.symmetricGeneralizedEigenvalues(result[0], cov, true);

        return result;
    }


}
