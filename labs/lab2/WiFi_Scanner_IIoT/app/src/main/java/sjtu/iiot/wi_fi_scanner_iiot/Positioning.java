package sjtu.iiot.wi_fi_scanner_iiot;

import android.util.Log;
import android.util.Pair;

import java.util.LinkedList;
import java.util.Random;

class Node {
    double x;
    double y;
    double a = 0;
    double f = 2400;

    public Node(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Node(double x, double y, double f, double a) {
        this.x = x;
        this.y = y;
        this.f = f;
        this.a = a;
    }

    public void move_to(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

public class Positioning {

    private static double alpha = 0.0001;   // suggested learning rate
    private static double beta = 6;         // environment medium loss
    private static int calibrate_scan_time = 100;   // scan times for beta
    private static int position_scan_time = 10;     // scan times for position
    private static double x1 = 0;       // do not modify
    private static double y1 = 0;       // do not modify
    private static double f1 = 2400;    // frequency
    private static double a1 = 0;       // power
    private static double x2 = 0.12;
    private static double y2 = 0;       // do not modify
    private static double f2 = 2400;
    private static double a2 = 0;
    private static double x3 = 0.06;
    private static double y3 = 0.13;
    private static double f3 = 2400;
    private static double a3 = 0;
    private static double x4 = 0.08;
    private static double y4 = 0.07;
    private static double f4 = 2400;
    private static double a4 = 0;
    private static double x0 = 0.03;    // device coord
    private static double y0 = 0.1;     // device coord

    LinkedList<Node> APs_for_beta;  // contain A1-A4
    LinkedList<Node> APs;           // contain A1-A3
    LinkedList<Node> APs_and_device;// contain A1-A4 and device
    LinkedList<Node> all_nodes;     // contain A1-A4, device and result
    Node device, result;
    boolean beta_get;
    double beta_hat;

    Positioning() {
        init_nodes();
        beta_get = false;
    }

    // for real-scene localization
    Node position(LinkedList<LinkedList<Double>> real_a_hats) {
        if (!beta_get) {
            beta_hat = estimate_beta(get_previous(real_a_hats, 4));
            beta_get = true;
        }
        result = localize(beta_hat, get_previous(real_a_hats, 3));
        // if already have position result, delete the result
        if (all_nodes.size() == 6) {
            all_nodes.removeLast();
        }
        all_nodes.add(result);
        return result;
    }

    // for simulated localization
    Node position() {
        if (!beta_get) {
            beta_hat = estimate_beta();
            beta_get = true;
        }
        result = localize(beta_hat);
        // if already have position result, delete the result
        if (all_nodes.size() == 6) {
            all_nodes.removeLast();
        }
        all_nodes.add(result);
        return result;
    }

    private Node localize(double beta_hat) {
        LinkedList<Double> a1_hat = simulate_scan(APs.get(0), beta,
                position_scan_time, true);
        LinkedList<Double> a2_hat = simulate_scan(APs.get(1), beta,
                position_scan_time, true);
        LinkedList<Double> a3_hat = simulate_scan(APs.get(2), beta,
                position_scan_time, true);
        LinkedList<LinkedList<Double>> a_hats = new LinkedList<>();
        a_hats.add(a1_hat);
        a_hats.add(a2_hat);
        a_hats.add(a3_hat);
        double x_hat = (new Random()).nextDouble();
        double y_hat = (new Random()).nextDouble();
        Node node = optimize(x_hat, y_hat, beta_hat, a_hats, alpha);
        return node;
    }

    private Node localize(double beta_hat,
                          LinkedList<LinkedList<Double>> a_hats) {
        double x_hat = (new Random()).nextDouble();
        double y_hat = (new Random()).nextDouble();
        Node node = optimize(x_hat, y_hat, beta_hat, a_hats, alpha);
        return node;
    }

    private Node optimize(double x_hat, double y_hat, double beta_hat,
                          LinkedList<LinkedList<Double>> a_hats, double alpha) {
        double previous_loss = loss(x_hat, y_hat, beta_hat, a_hats);
        double progress = 1;
        double x = x_hat;
        double y = y_hat;
        while (progress > 0.000001) {
            Pair<Double, Double> gradient = gradients(x, y,
                    beta_hat, a_hats);
            x -= alpha * gradient.first;
            y -= alpha * gradient.second;
            double new_loss = loss(x, y, beta_hat, a_hats);
            progress = previous_loss - new_loss;
            previous_loss = new_loss;
        }
        return new Node(x, y);
    }

    private double loss(double x_hat, double y_hat, double beta_hat,
                        LinkedList<LinkedList<Double>> a_hats) {
        double loss = 0;
        for (int i = 0; i < a_hats.size(); i++) {
            double diff_x = x_hat - APs.get(i).x;
            double diff_y = y_hat - APs.get(i).y;
            double d = -APs.get(i).a + 32.45 +
                    20 * Math.log10(APs.get(i).f) + beta_hat;
            for (int j = 0; j < a_hats.get(0).size(); j++) {
                double c_ij = (a_hats.get(i).get(j) + d) * Math.log(10) / 10;
                double res = Math.log(Math.pow(diff_x, 2) + Math.pow(diff_y, 2))
                        + c_ij;
                loss += Math.pow(res, 2);
            }
        }
        return loss / a_hats.size() / a_hats.get(0).size();
    }

    private Pair<Double, Double> gradients(double x_hat, double y_hat,
                                           double beta_hat,
                                           LinkedList<LinkedList<Double>> a_hats) {
        double fx = 0;
        double fy = 0;
        for (int i = 0; i < a_hats.size(); i++) {
            double diff_x = x_hat - APs.get(i).x;
            double diff_y = y_hat - APs.get(i).y;
            int n = a_hats.get(0).size();
            double square_sum = Math.pow(diff_x, 2) + Math.pow(diff_y, 2);
            double coef_x = 4 * diff_x / square_sum;
            double coef_y = 4 * diff_y / square_sum;
            fx += coef_x * n * Math.log(square_sum);
            fy += coef_y * n * Math.log(square_sum);
            double c_i = n * (32.45 - APs.get(i).a +
                    20 * Math.log10(APs.get(i).f) + beta_hat);
            c_i += sum(a_hats.get(i));
            fx += coef_x * Math.log(10) / 10 * c_i;
            fy += coef_y * Math.log(10) / 10 * c_i;
        }
        fx /= (a_hats.size() * a_hats.get(0).size());
        fy /= (a_hats.size() * a_hats.get(0).size());
        return new Pair<>(fx, fy);
    }

    private double estimate_beta() {
        double a1_bar = mean(simulate_scan(APs_for_beta.get(0), beta,
                calibrate_scan_time, true));
        double a2_bar = mean(simulate_scan(APs_for_beta.get(1), beta,
                calibrate_scan_time, true));
        double a3_bar = mean(simulate_scan(APs_for_beta.get(2), beta,
                calibrate_scan_time, true));
        double a4_bar = mean(simulate_scan(APs_for_beta.get(3), beta,
                calibrate_scan_time, true));
        LinkedList<Double> solutions = solve_beta(a1_bar, a2_bar, a3_bar,
                a4_bar, APs_for_beta.get(0), APs_for_beta.get(1),
                APs_for_beta.get(2), APs_for_beta.get(3));
        double x1 = solutions.get(0);
        double y1 = solutions.get(1);
        double x2 = solutions.get(2);
        double y2 = solutions.get(3);
        double beta1 = solutions.get(4);
        double beta2 = solutions.get(5);
        double beta1_4 = solutions.get(6);
        double beta2_4 = solutions.get(7);
        double delta_beta1 = Math.abs(beta1 - beta1_4);
        double delta_beta2 = Math.abs(beta2 - beta2_4);
        return delta_beta1 < delta_beta2 ? beta1 : beta2;
    }

    private double estimate_beta(LinkedList<LinkedList<Double>> a_hats) {
        double a1_bar = mean(a_hats.get(0));
        double a2_bar = mean(a_hats.get(1));
        double a3_bar = mean(a_hats.get(2));
        double a4_bar = mean(a_hats.get(3));
        LinkedList<Double> solutions = solve_beta(a1_bar, a2_bar, a3_bar,
                a4_bar, APs_for_beta.get(0), APs_for_beta.get(1),
                APs_for_beta.get(2), APs_for_beta.get(3));
        double x1 = solutions.get(0);
        double y1 = solutions.get(1);
        double x2 = solutions.get(2);
        double y2 = solutions.get(3);
        double beta1 = solutions.get(4);
        double beta2 = solutions.get(5);
        double beta1_4 = solutions.get(6);
        double beta2_4 = solutions.get(7);
        double delta_beta1 = Math.abs(beta1 - beta1_4);
        double delta_beta2 = Math.abs(beta2 - beta2_4);
        return delta_beta1 < delta_beta2 ? beta1 : beta2;
    }

    private LinkedList<Double> simulate_scan(Node AP, double beta_local,
                                             int scan_time, boolean noise_flag) {
        double noise = noise_flag ? (new Random()).nextGaussian() * 0.05 : 0;
        LinkedList<Double> RSSs = new LinkedList<>();
        for (int i = 0; i < scan_time; i++) {
            double RSS = AP.a - 32.45 - 20 * Math.log10(AP.f) - 10 * Math.log10(
                    Math.pow(AP.x - device.x, 2) + Math.pow(AP.y - device.y, 2))
                    - beta_local + noise;
            RSSs.add(RSS);
        }
        return RSSs;
    }

    private LinkedList<Double> solve_beta(double a1_bar, double a2_bar,
                                          double a3_bar, double a4_bar,
                                          Node A1, Node A2, Node A3, Node A4) {
        double k1 = Math.pow(10, (A2.a - A1.a - a2_bar + a1_bar -
                20 * Math.log10(A2.f / A1.f)) / 10);
        double k2 = Math.pow(10, (A3.a - A1.a - a3_bar + a1_bar -
                20 * Math.log10(A3.f / A1.f)) / 10);
        double m = -((k1 - 1) * A3.x - k2 * A2.x + A2.x) / A3.y;
        double n =
                ((Math.pow(A3.x, 2) + Math.pow(A3.y, 2)) * (k1 - 1) -
                        (k2 - 1) * Math.pow(A2.x, 2)) / (2 * A3.y);
        double a = k1 - 1 + Math.pow(m, 2) / (k1 - 1);
        double b = 2 / (k1 - 1) * m *n + 2 * A2.x;
        double c = Math.pow(n, 2) / (k1 - 1) - Math.pow(A2.x, 2);
        double delta = Math.pow(b, 2) - 4 * a * c;
        double x1 = (-b + Math.sqrt(delta)) / (2 * a);
        double y1 = m / (k1 - 1) * x1 + n / (k1 - 1);
        double x2 = (-b - Math.sqrt(delta)) / (2 * a);
        double y2 = m / (k1 - 1) * x2 + n / (k1 - 1);
        double beta1 = calculate_beta_by_xy(A1, a1_bar, x1, y1);
        double beta2 = calculate_beta_by_xy(A1, a1_bar, x2, y2);
        double beta1_4 = calculate_beta_by_xy(A4, a4_bar, x1, y1);
        double beta2_4 = calculate_beta_by_xy(A4, a4_bar, x2, y2);
        LinkedList<Double> result = new LinkedList<>();
        result.add(x1);
        result.add(y1);
        result.add(x2);
        result.add(y2);
        result.add(beta1);
        result.add(beta2);
        result.add(beta1_4);
        result.add(beta2_4);
        return result;
    }

    private double calculate_beta_by_xy(Node AP, double a_bar,
                                        double x, double y) {
        return AP.a - a_bar - 32.45 - 20 * Math.log10(AP.f) - 10 * Math.log10(
                Math.pow(x - AP.x, 2) + Math.pow(y - AP.y, 2));
    }

    private double sum(LinkedList<Double> list) {
        double res = 0;
        for (int i = 0; i < list.size(); i++) {
            res += list.get(i);
        }
        return res;
    }

    private double mean(LinkedList<Double> list) {
        return list.size() == 0 ? 0 : sum(list) / list.size();
    }

    private void init_nodes() {
        APs = new LinkedList<>();
        APs.add(new Node(x1, y1, f1, a1));
        APs.add(new Node(x2, y2, f2, a2));
        APs.add(new Node(x3, y3, f3, a3));
        APs_for_beta = new LinkedList<>(APs);
        APs_for_beta.add(new Node(x4, y4, f4, a4));
        APs_and_device = new LinkedList<>(APs_for_beta);
        device = new Node(x0, y0);
        APs_and_device.add(device);
        all_nodes = new LinkedList<>(APs_and_device);
    }

    private LinkedList<LinkedList<Double>> get_previous(
            LinkedList<LinkedList<Double>> list, int i
    ) {
        LinkedList<LinkedList<Double>> result = new LinkedList<>();
        for (int k = 0; k < i; k++) {
            result.add(list.get(k));
        }
        return result;
    }
}
