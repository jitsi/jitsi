/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.silk;

public class Silk_solve_LS_FLP
{
    /**
     * Function to solve linear equation Ax = b, when A is a MxM  
     * symmetric square matrix - using LDL factorisation.
     * @param A Symmetric square matrix, out: reg.
     * @param A_offset offset of valid data.
     * @param M Size of matrix
     * @param b Pointer to b vector
     * @param x Pointer to x solution vector
     * @param x_offset offset of valid data.
     */
    static void SKP_Silk_solve_LDL_FLP(
              float                 []A,                 /* I/O  Symmetric square matrix, out: reg.      */
              int                   A_offset,
        final int                   M,                  /* I    Size of matrix                          */
        final float                 []b,                 /* I    Pointer to b vector                     */
              float                 []x,                  /* O    Pointer to x solution vector            */
              int                   x_offset
    )   
    {
        int i;
//        float L[][] = new float[Silk_define.MAX_MATRIX_SIZE][Silk_define.MAX_MATRIX_SIZE];
//TODO:change L from two dimension to one dimension.
        float L_tmp[] = new float[Silk_define.MAX_MATRIX_SIZE*Silk_define.MAX_MATRIX_SIZE];
        float T[] = new float[Silk_define.MAX_MATRIX_SIZE];
        float Dinv[] = new float[Silk_define.MAX_MATRIX_SIZE];// inverse diagonal elements of D

        assert( M <= Silk_define.MAX_MATRIX_SIZE );

        /***************************************************
        Factorize A by LDL such that A = L*D*(L^T),
        where L is lower triangular with ones on diagonal
        ****************************************************/
//        SKP_Silk_LDL_FLP( A, M, &L[ 0 ][ 0 ], Dinv );
        SKP_Silk_LDL_FLP(A, A_offset, M, L_tmp, Dinv);

        /****************************************************
        * substitute D*(L^T) = T. ie:
        L*D*(L^T)*x = b => L*T = b <=> T = inv(L)*b
        ******************************************************/
//        SKP_Silk_SolveWithLowerTriangularWdiagOnes_FLP( &L[ 0 ][ 0 ], M, b, T );
        SKP_Silk_SolveWithLowerTriangularWdiagOnes_FLP( L_tmp, M, b, T );

        /****************************************************
        D*(L^T)*x = T <=> (L^T)*x = inv(D)*T, because D is 
        diagonal just multiply with 1/d_i
        ****************************************************/
        for( i = 0; i < M; i++ ) {
            T[ i ] = T[ i ] * Dinv[ i ];
        }
        /****************************************************
        x = inv(L') * inv(D) * T
        *****************************************************/
//        SKP_Silk_SolveWithUpperTriangularFromLowerWdiagOnes_FLP( &L[ 0 ][ 0 ], M, T, x );
        SKP_Silk_SolveWithUpperTriangularFromLowerWdiagOnes_FLP( L_tmp, M, T, x, x_offset );

    }

    /**
     * Function to solve linear equation (A^T)x = b, when A is a MxM lower 
     * triangular, with ones on the diagonal. (ie then A^T is upper triangular)
     * @param L Pointer to Lower Triangular Matrix
     * @param M Dim of Matrix equation
     * @param b b Vector
     * @param x x Vector
     * @param x_offset offset of valid data.
     */
    static void SKP_Silk_SolveWithUpperTriangularFromLowerWdiagOnes_FLP(
        final float     []L,     /* (I) Pointer to Lower Triangular Matrix */
        int             M,      /* (I) Dim of Matrix equation */
        final float     []b,     /* (I) b Vector */
        float           []x,      /* (O) x Vector */  
        int             x_offset
    )
    {
//        SKP_int   i, j;
//        SKP_float temp;
//        const SKP_float *ptr1;
        int i,j;
        float temp;
//TODO:ignore const        
        float []ptr1;
        int ptr1_offset;
        
        for( i = M - 1; i >= 0; i-- ) {
//            ptr1 =  matrix_adr( L, 0, i, M );
            ptr1 = L;
            ptr1_offset = i;
            temp = 0;
            for( j = M - 1; j > i ; j-- ) {
                temp += ptr1[ ptr1_offset + j * M ] * x[ x_offset + j ];
            }
            temp = b[ i ] - temp;
            x[ x_offset + i ] = temp;      
        }
    }

    /**
     * Function to solve linear equation Ax = b, when A is a MxM lower 
     * triangular matrix, with ones on the diagonal.
     * @param L Pointer to Lower Triangular Matrix
     * @param M Pointer to Lower Triangular Matrix
     * @param b b Vector
     * @param x x Vector
     */
    static void SKP_Silk_SolveWithLowerTriangularWdiagOnes_FLP(
        final float     []L,     /* (I) Pointer to Lower Triangular Matrix */
        int             M,      /* (I) Pointer to Lower Triangular Matrix */
        final float     []b,     /* (I) b Vector */
        float           []x      /* (O) x Vector */  
    )
    {
//        SKP_int   i, j;
//        SKP_float temp;
//        const SKP_float *ptr1;
        int i,j;
        float temp;
//TODO:ignore const  
        float []ptr1;
        int ptr1_offset;
        
        for( i = 0; i < M; i++ ) {
//            ptr1 =  matrix_adr( L, i, 0, M );
            ptr1 = L;
            ptr1_offset = i*M;
            temp = 0;
            for( j = 0; j < i; j++ ) {
                temp += ptr1[ ptr1_offset + j ] * x[ j ];
            }
            temp = b[ i ] - temp;
            x[ i ] = temp;
        }
    }
    
    /**
     * LDL Factorisation. Finds the upper triangular matrix L and the diagonal
     * Matrix D (only the diagonal elements returned in a vector)such that 
     * the symmetric matric A is given by A = L*D*L'.
     * @param A Pointer to Symetric Square Matrix
     * @param A_offset offset of valid data.
     * @param M Size of Matrix
     * @param L Pointer to Square Upper triangular Matrix 
     * @param Dinv Pointer to vector holding the inverse diagonal elements of D
     */
    static void SKP_Silk_LDL_FLP(
        float           []A,      /* (I/O) Pointer to Symetric Square Matrix */
        int             A_offset,
        int             M,       /* (I) Size of Matrix */
        float           []L,      /* (I/O) Pointer to Square Upper triangular Matrix */
        float           []Dinv    /* (I/O) Pointer to vector holding the inverse diagonal elements of D */
    )
    {
/*        SKP_int i, j, k, loop_count, err = 1;
        SKP_float *ptr1, *ptr2;
        double temp, diag_min_value;
        SKP_float v[ MAX_MATRIX_SIZE ], D[ MAX_MATRIX_SIZE ]; // temp arrays
*/
        int i, j, k, loop_count, err = 1;
        float ptr1[], ptr2[];
        int ptr1_offset, ptr2_offset;
        double temp, diag_min_value;
        float v[] = new float[ Silk_define.MAX_MATRIX_SIZE ], D[] = new float[ Silk_define.MAX_MATRIX_SIZE ]; // temp arrays
        
        assert( M <= Silk_define.MAX_MATRIX_SIZE );

        diag_min_value = Silk_define_FLP.FIND_LTP_COND_FAC * 0.5f * ( A[ A_offset + 0 ] + A[ A_offset + M * M - 1 ] ); 
        for( loop_count = 0; loop_count < M && err == 1; loop_count++ ) {
            err = 0;
            for( j = 0; j < M; j++ ) {
//                ptr1 = matrix_adr( L, j, 0, M );
//                temp = matrix_ptr( A, j, j, M ); // element in row j column j
                ptr1 = L;
                ptr1_offset = j*M + 0;
                temp = A[A_offset + j*M + j];
                for( i = 0; i < j; i++ ) {
                    v[ i ] = ptr1[ ptr1_offset + i ] * D[ i ];
                    temp  -= ptr1[ ptr1_offset + i ] * v[ i ];
                }
                if( temp < diag_min_value ) {
                    /* Badly conditioned matrix: add white noise and run again */
                    temp = ( loop_count + 1 ) * diag_min_value - temp;
                    for( i = 0; i < M; i++ ) {
//                        matrix_ptr( A, i, i, M ) += ( SKP_float )temp;
                        A[A_offset + i*M+i] += temp;
                    }
                    err = 1;
                    break;
                }
                D[ j ]    = ( float )temp;
                Dinv[ j ] = ( float )( 1.0f / temp );
//                matrix_ptr( L, j, j, M ) = 1.0f;
                L[j*M+j] = 1.0f;
                
//                ptr1 = matrix_adr( A, j, 0, M );
//                ptr2 = matrix_adr( L, j + 1, 0, M);
                ptr1 = A;
                ptr1_offset = A_offset + j*M;
                ptr2 = L;
                ptr2_offset = (j+1)*M;
                
                for( i = j + 1; i < M; i++ ) {
                    temp = 0.0;
                    for( k = 0; k < j; k++ ) {                
                        temp += ptr2[ ptr2_offset + k ] * v[ k ];
                    }
//                    matrix_ptr( L, i, j, M ) = ( SKP_float )( ( ptr1[ i ] - temp ) * Dinv[ j ] );
                    L[i*M+j] = (float) ( ( ptr1[ ptr1_offset + i ] - temp ) * Dinv[ j ] );
                    ptr2_offset += M; // go to next column
                }   
            }
        }
        assert( err == 0 );
    }
}
