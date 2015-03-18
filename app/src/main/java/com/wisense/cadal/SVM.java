package com.wisense.cadal;

import android.content.Context;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

/**
 * Created by lucapernini on 12/03/15.
 */
public class SVM {

    static String TAG="FALL_DETECTION";

    Context cont;

    private svm_problem prob;
    private svm_parameter param;
    private int cross_validation;
    private int nr_fold;
    private String error_msg;

    private svm_model model;
    private String input_file_name;		// set by parse_command_line
    private String model_file_name="fall_training_set___.model";

    static final String svm_type_table[] =
            {
                    "c_svc","nu_svc","one_class","epsilon_svr","nu_svr",
            };
    static final String kernel_type_table[]=
            {
                    "linear","polynomial","rbf","sigmoid","precomputed"
            };


    public SVM(Context context) {
        cont=context;
    }

    private void read_problem() throws IOException
    {
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
        File file = new File(dir, "fall_training_set___new");

        InputStream inputStream;
        BufferedReader fp;

        if(file.exists())
        {
            Log.d(TAG,"SVM: generating model based on new training set");
            inputStream=new FileInputStream(file);
            fp=new BufferedReader(new FileReader(file));
        } else {
            Log.d(TAG,"SVM: generating model based on default training set");
            inputStream = cont.getResources().openRawResource(R.raw.fall_training_set___);
            fp = new BufferedReader(new InputStreamReader(inputStream));
        }
        //BufferedReader fp = new BufferedReader(new FileReader(input_file_name));
        Vector<Double> vy = new Vector<Double>();
        Vector<svm_node[]> vx = new Vector<svm_node[]>();
        int max_index = 0;

        while(true)
        {
            String line = fp.readLine();
            if(line == null) break;

            StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

            vy.addElement(atof(st.nextToken()));
            int m = st.countTokens()/2;
            svm_node[] x = new svm_node[m];
            for(int j=0;j<m;j++)
            {
                x[j] = new svm_node();
                x[j].index = atoi(st.nextToken());
                x[j].value = atof(st.nextToken());
            }
            if(m>0) max_index = Math.max(max_index, x[m-1].index);
            vx.addElement(x);
        }

        prob = new svm_problem();
        prob.l = vy.size();
        prob.x = new svm_node[prob.l][];
        for(int i=0;i<prob.l;i++)
            prob.x[i] = vx.elementAt(i);
        prob.y = new double[prob.l];
        for(int i=0;i<prob.l;i++)
            prob.y[i] = vy.elementAt(i);

        if(param.gamma == 0 && max_index > 0)
            param.gamma = 1.0/max_index;

        if(param.kernel_type == svm_parameter.PRECOMPUTED)
            for(int i=0;i<prob.l;i++)
            {
                if (prob.x[i][0].index != 0)
                {
                    System.err.print("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
                    System.exit(1);
                }
                if ((int)prob.x[i][0].value <= 0 || (int)prob.x[i][0].value > max_index)
                {
                    System.err.print("Wrong input format: sample_serial_number out of range\n");
                    System.exit(1);
                }
            }

        fp.close();

    }

    private static double atof(String s)
    {
        double d = Double.valueOf(s).doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d))
        {
            System.err.print("NaN or Infinity in input\n");
            System.exit(1);
        }
        return(d);
    }

    private static int atoi(String s)
    {
        return Integer.parseInt(s);
    }

    private void set_parameters(){
        param = new svm_parameter();
        // default values
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.degree = 3;
        param.gamma = 0;	// 1/num_features
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = 1;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];
        cross_validation = 0;
    }

    private void do_cross_validation()
    {
        int i;
        int total_correct = 0;
        double total_error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
        double[] target = new double[prob.l];

        svm.svm_cross_validation(prob, param, nr_fold, target);
        if(param.svm_type == svm_parameter.EPSILON_SVR ||
                param.svm_type == svm_parameter.NU_SVR)
        {
            for(i=0;i<prob.l;i++)
            {
                double y = prob.y[i];
                double v = target[i];
                total_error += (v-y)*(v-y);
                sumv += v;
                sumy += y;
                sumvv += v*v;
                sumyy += y*y;
                sumvy += v*y;
            }
            System.out.print("Cross Validation Mean squared error = "+total_error/prob.l+"\n");
            System.out.print("Cross Validation Squared correlation coefficient = "+
                            ((prob.l*sumvy-sumv*sumy)*(prob.l*sumvy-sumv*sumy))/
                                    ((prob.l*sumvv-sumv*sumv)*(prob.l*sumyy-sumy*sumy))+"\n"
            );
        }
        else
        {
            for(i=0;i<prob.l;i++)
                if(target[i] == prob.y[i])
                    ++total_correct;
            System.out.print("Cross Validation Accuracy = "+100.0*total_correct/prob.l+"%\n");
        }
    }

    public void train(boolean update_svm) throws IOException{

        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
        File file = new File(dir, "fall_training_set___.model");

        if(!file.exists()||update_svm)

        {
            set_parameters();
            read_problem();
            error_msg = svm.svm_check_parameter(prob, param);
            if (error_msg != null) {
                System.err.print("ERROR: " + error_msg + "\n");
                System.exit(1);
            }

            if (cross_validation != 0) {
                do_cross_validation();
            } else {
                model = svm.svm_train(prob, param);
                svm_save_model(model_file_name, model);
            }
        }
    }

    public static void svm_save_model(String model_file_name, svm_model model) throws IOException
    {
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
        dir.mkdirs();
        File file = new File(dir, "fall_training_set___.model");

        DataOutputStream fp = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file,true)));

        svm_parameter param = model.param;

        fp.writeBytes("svm_type "+svm_type_table[param.svm_type]+"\n");
        fp.writeBytes("kernel_type "+kernel_type_table[param.kernel_type]+"\n");

        if(param.kernel_type == svm_parameter.POLY)
            fp.writeBytes("degree "+param.degree+"\n");

        if(param.kernel_type == svm_parameter.POLY ||
                param.kernel_type == svm_parameter.RBF ||
                param.kernel_type == svm_parameter.SIGMOID)
            fp.writeBytes("gamma "+param.gamma+"\n");

        if(param.kernel_type == svm_parameter.POLY ||
                param.kernel_type == svm_parameter.SIGMOID)
            fp.writeBytes("coef0 "+param.coef0+"\n");

        int nr_class = model.nr_class;
        int l = model.l;
        fp.writeBytes("nr_class "+nr_class+"\n");
        fp.writeBytes("total_sv "+l+"\n");

        {
            fp.writeBytes("rho");
            for(int i=0;i<nr_class*(nr_class-1)/2;i++)
                fp.writeBytes(" "+model.rho[i]);
            fp.writeBytes("\n");
        }

        if(model.label != null)
        {
            fp.writeBytes("label");
            for(int i=0;i<nr_class;i++)
                fp.writeBytes(" "+model.label[i]);
            fp.writeBytes("\n");
        }

        if(model.probA != null) // regression has probA only
        {
            fp.writeBytes("probA");
            for(int i=0;i<nr_class*(nr_class-1)/2;i++)
                fp.writeBytes(" "+model.probA[i]);
            fp.writeBytes("\n");
        }
        if(model.probB != null)
        {
            fp.writeBytes("probB");
            for(int i=0;i<nr_class*(nr_class-1)/2;i++)
                fp.writeBytes(" "+model.probB[i]);
            fp.writeBytes("\n");
        }

        if(model.nSV != null)
        {
            fp.writeBytes("nr_sv");
            for(int i=0;i<nr_class;i++)
                fp.writeBytes(" "+model.nSV[i]);
            fp.writeBytes("\n");
        }

        fp.writeBytes("SV\n");
        double[][] sv_coef = model.sv_coef;
        svm_node[][] SV = model.SV;

        for(int i=0;i<l;i++)
        {
            for(int j=0;j<nr_class-1;j++)
                fp.writeBytes(sv_coef[j][i]+" ");

            svm_node[] p = SV[i];
            if(param.kernel_type == svm_parameter.PRECOMPUTED)
                fp.writeBytes("0:"+(int)(p[0].value));
            else
                for(int j=0;j<p.length;j++)
                    fp.writeBytes(p[j].index+":"+p[j].value+" ");
            fp.writeBytes("\n");
        }

        fp.close();
    }

    private static boolean read_model_header(BufferedReader fp, svm_model model)
    {
        svm_parameter param = new svm_parameter();
        model.param = param;
        try
        {
            while(true)
            {
                String cmd = fp.readLine();
                String arg = cmd.substring(cmd.indexOf(' ')+1);

                if(cmd.startsWith("svm_type"))
                {
                    int i;
                    for(i=0;i<svm_type_table.length;i++)
                    {
                        if(arg.indexOf(svm_type_table[i])!=-1)
                        {
                            param.svm_type=i;
                            break;
                        }
                    }
                    if(i == svm_type_table.length)
                    {
                        System.err.print("unknown svm type.\n");
                        return false;
                    }
                }
                else if(cmd.startsWith("kernel_type"))
                {
                    int i;
                    for(i=0;i<kernel_type_table.length;i++)
                    {
                        if(arg.indexOf(kernel_type_table[i])!=-1)
                        {
                            param.kernel_type=i;
                            break;
                        }
                    }
                    if(i == kernel_type_table.length)
                    {
                        System.err.print("unknown kernel function.\n");
                        return false;
                    }
                }
                else if(cmd.startsWith("degree"))
                    param.degree = atoi(arg);
                else if(cmd.startsWith("gamma"))
                    param.gamma = atof(arg);
                else if(cmd.startsWith("coef0"))
                    param.coef0 = atof(arg);
                else if(cmd.startsWith("nr_class"))
                    model.nr_class = atoi(arg);
                else if(cmd.startsWith("total_sv"))
                    model.l = atoi(arg);
                else if(cmd.startsWith("rho"))
                {
                    int n = model.nr_class * (model.nr_class-1)/2;
                    model.rho = new double[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for(int i=0;i<n;i++)
                        model.rho[i] = atof(st.nextToken());
                }
                else if(cmd.startsWith("label"))
                {
                    int n = model.nr_class;
                    model.label = new int[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for(int i=0;i<n;i++)
                        model.label[i] = atoi(st.nextToken());
                }
                else if(cmd.startsWith("probA"))
                {
                    int n = model.nr_class*(model.nr_class-1)/2;
                    model.probA = new double[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for(int i=0;i<n;i++)
                        model.probA[i] = atof(st.nextToken());
                }
                else if(cmd.startsWith("probB"))
                {
                    int n = model.nr_class*(model.nr_class-1)/2;
                    model.probB = new double[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for(int i=0;i<n;i++)
                        model.probB[i] = atof(st.nextToken());
                }
                else if(cmd.startsWith("nr_sv"))
                {
                    int n = model.nr_class;
                    model.nSV = new int[n];
                    StringTokenizer st = new StringTokenizer(arg);
                    for(int i=0;i<n;i++)
                        model.nSV[i] = atoi(st.nextToken());
                }
                else if(cmd.startsWith("SV"))
                {
                    break;
                }
                else
                {
                    System.err.print("unknown text in model file: ["+cmd+"]\n");
                    return false;
                }
            }
        }
        catch(Exception e)
        {
            return false;
        }
        return true;
    }

    public static svm_model svm_load_model(BufferedReader fp) throws IOException
    {
        // read parameters

        svm_model model = new svm_model();
        model.rho = null;
        model.probA = null;
        model.probB = null;
        model.label = null;
        model.nSV = null;

        if (read_model_header(fp, model) == false)
        {
            System.err.print("ERROR: failed to read model\n");
            return null;
        }

        // read sv_coef and SV

        int m = model.nr_class - 1;
        int l = model.l;
        model.sv_coef = new double[m][l];
        model.SV = new svm_node[l][];

        for(int i=0;i<l;i++)
        {
            String line = fp.readLine();
            StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

            for(int k=0;k<m;k++)
                model.sv_coef[k][i] = atof(st.nextToken());
            int n = st.countTokens()/2;
            model.SV[i] = new svm_node[n];
            for(int j=0;j<n;j++)
            {
                model.SV[i][j] = new svm_node();
                model.SV[i][j].index = atoi(st.nextToken());
                model.SV[i][j].value = atof(st.nextToken());
            }
        }

        fp.close();
        return model;
    }

    public boolean read_test(String test) throws IOException{

        boolean fall=false;

        int predict_probability=0;

        try
        {

            //BufferedReader input = new BufferedReader(new FileReader(argv[i]));
            // convert String into InputStream
            InputStream is = new ByteArrayInputStream(test.getBytes());
            // read it with BufferedReader
            BufferedReader input = new BufferedReader(new InputStreamReader(is));

            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
            dir.mkdirs();
            File file = new File(dir, getDate()+"_CadAl_result");
            File fileModel = new File(dir, model_file_name);
            FileOutputStream fileOut = new FileOutputStream(file,true);
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(fileOut));
            BufferedReader modelBuffer = new BufferedReader(new FileReader(fileModel));
            //InputStream modelStream = cont.getResources().openRawResource(R.raw.fall_training_set___);
            //BufferedReader modelBufferedReader = new BufferedReader(new InputStreamReader(modelStream)); //TODO STO LEGGENDO IL SET DI TRAINING NON IL MODEL INFATTI DA ERRORE!

            svm_model model = svm.svm_load_model(modelBuffer);
            if (model == null)
            {
                System.err.print("can't open model file ");
                System.exit(1);
            }
            if(predict_probability == 1)
            {
                if(svm.svm_check_probability_model(model)==0)
                {
                    System.err.print("Model does not support probabiliy estimates\n");
                    System.exit(1);
                }
            }
            else
            {
                if(svm.svm_check_probability_model(model)!=0)
                {
                   // svm_predict.info("Model supports probability estimates, but disabled in prediction.\n");
                }
            }
            fall=predict(input,output,model,predict_probability);
            //input.close();
            //output.close();
        }
        catch(FileNotFoundException e)
        {
            //exit_with_help();
        }
        catch(ArrayIndexOutOfBoundsException e)
        {

        }
        return fall;
    }

    private static boolean predict(BufferedReader input, DataOutputStream output, svm_model model, int predict_probability)
    {
        Log.d(TAG,"SVM: predicting");
        boolean fall_detected=false;
        int correct = 0;
        int total = 0;
        double error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;

        int svm_type=svm.svm_get_svm_type(model);
        int nr_class=svm.svm_get_nr_class(model);
        double[] prob_estimates=null;

        if(predict_probability == 1)
        {
            //Log.d(TAG,"rigo 542");
            if(svm_type == svm_parameter.EPSILON_SVR ||
                    svm_type == svm_parameter.NU_SVR)
            {
                //svm_predict.info("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma="+svm.svm_get_svr_probability(model)+"\n");
            }
            else
            {
                int[] labels=new int[nr_class];
                svm.svm_get_labels(model,labels);
                prob_estimates = new double[nr_class];
                try {
                    output.writeBytes("labels");
                    for(int j=0;j<nr_class;j++) {
                        output.writeBytes(" " + labels[j]);
//                        if(labels[j]==2) fall_detected=true;
//                        Log.d(TAG, "LABEL: " + labels[j]);
                    }
                    output.writeBytes("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        while(true)
        {
            try {
                //Log.d(TAG,"rigo 569");
                String line = input.readLine();
                if(line == null) break;

                StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

                double target = atof(st.nextToken());
                int m = st.countTokens()/2;
                svm_node[] x = new svm_node[m];
                for(int j=0;j<m;j++)
                {
                    x[j] = new svm_node();
                    x[j].index = atoi(st.nextToken());
                    x[j].value = atof(st.nextToken());
                }

                double v;
                if (predict_probability==1 && (svm_type==svm_parameter.C_SVC || svm_type==svm_parameter.NU_SVC))
                {
                    //Log.d(TAG,"rigo586"); QUI NON ENTRA
                    v = svm.svm_predict_probability(model,x,prob_estimates);
                    Log.d(TAG,"v: "+v);
                    try {
                        output.writeBytes(v+" ");
                        for(int j=0;j<nr_class;j++) {
                            output.writeBytes(prob_estimates[j] + " ");

                        }
                        output.writeBytes("\n");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    v = svm.svm_predict(model, x);
                    try {
                        Log.d(TAG,"svm_predict: v="+v);
                        if(v==1) fall_detected=true;

                        output.writeBytes(v+"\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(v == target)
                    ++correct;
                error += (v-target)*(v-target);
                sumv += v;
                sumy += target;
                sumvv += v*v;
                sumyy += target*target;
                sumvy += v*target;
                ++total;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(svm_type == svm_parameter.EPSILON_SVR ||
                svm_type == svm_parameter.NU_SVR)
        {
            //svm_predict.info("Mean squared error = "+error/total+" (regression)\n");
            //svm_predict.info("Squared correlation coefficient = "+
              //      ((total*sumvy-sumv*sumy)*(total*sumvy-sumv*sumy))/
                //            ((total*sumvv-sumv*sumv)*(total*sumyy-sumy*sumy))+
                  //  " (regression)\n");
        }
        else {
            //svm_predict.info("Accuracy = "+(double)correct/total*100+
            //               "% ("+correct+"/"+total+") (classification)\n");
        }
        return fall_detected;
    }

    public String getDate(){

        Date date=new Date();
        String format="yyyyMMdd_HHmm";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ITALY);

        String now=sdf.format(date);
        return now;
    }

}
