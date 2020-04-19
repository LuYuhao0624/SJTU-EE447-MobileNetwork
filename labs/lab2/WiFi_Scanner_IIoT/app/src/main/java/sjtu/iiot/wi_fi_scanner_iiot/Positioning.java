package sjtu.iiot.wi_fi_scanner_iiot;

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

    public Node(double x, double y, double a, double f) {
        this.x = x;
        this.y = y;
        this.a = a;
        this.f = f;
    }

    public void move_to(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

public class Positioning {
    private static int calibrate_scan_time = 100;
    private static double sigma = 0.01;
    private static double beta = 6;
    private static int position_scan_time = 100;

    public Node position(LinkedList<Node> APs, Node device)
    {
        LinkedList<Node> APs_for_beta = new LinkedList<>(APs);
        double beta_hat = estimate_beta(APs_for_beta, device);
        LinkedList<Node> APs_for_position = new LinkedList<>(APs);
        APs_for_position.removeLast();
        Node node = localize(APs_for_position, beta_hat, device);
        return node;
    }

    private Node localize(LinkedList<Node> APs, double beta_hat,
                                          Node device) {
        LinkedList<Double> a1_hat = simulate_scan(device, APs.get(0), beta,
                position_scan_time, true);
        LinkedList<Double> a2_hat = simulate_scan(device, APs.get(1), beta,
                position_scan_time, true);
        LinkedList<Double> a3_hat = simulate_scan(device, APs.get(2), beta,
                position_scan_time, true);
        LinkedList<LinkedList<Double>> a_hats = new LinkedList<>();
        a_hats.add(a1_hat);
        a_hats.add(a2_hat);
        a_hats.add(a3_hat);
        double x_hat = (new Random()).nextDouble();
        double y_hat = (new Random()).nextDouble();
        Node node = optimize(x_hat, y_hat, APs, beta_hat, a_hats, 0.0001);
        return node;
    }

    private Node optimize(double x_hat, double y_hat,
                                          LinkedList<Node> APs, double beta_hat,
                                          LinkedList<LinkedList<Double>> a_hats,
                                          double alpha) {
        double previous_loss = loss(x_hat, y_hat, APs, beta_hat, a_hats);
        double progress = 1;
        double x = x_hat;
        double y = y_hat;
        while (progress > 0.0001) {
            Pair<Double, Double> gradient = gradients(x, y, APs,
                    beta_hat, a_hats);
            x -= alpha * gradient.first;
            y -= alpha * gradient.second;
            double new_loss = loss(x, y, APs, beta_hat, a_hats);
            progress = previous_loss - new_loss;
            previous_loss = new_loss;
        }
        return new Node(x, y);
    }

    private double loss(double x_hat, double y_hat, LinkedList<Node> APs,
                        double beta_hat, LinkedList<LinkedList<Double>> a_hats) {
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
                                           LinkedList<Node> APs, double beta_hat,
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

    private double estimate_beta(LinkedList<Node> APs, Node device) {
        double a1_bar = mean(simulate_scan(device, APs.get(0), beta,
                calibrate_scan_time, true));
        double a2_bar = mean(simulate_scan(device, APs.get(1), beta,
                calibrate_scan_time, true));
        double a3_bar = mean(simulate_scan(device, APs.get(2), beta,
                calibrate_scan_time, true));
        double a4_bar = mean(simulate_scan(device, APs.get(3), beta,
                calibrate_scan_time, true));
        LinkedList<Double> solutions = solve_beta(a1_bar, a2_bar, a3_bar,
                a4_bar, APs.get(0), APs.get(1), APs.get(2), APs.get(3));
        double x1 = solutions.get(0);
        double y1 = solutions.get(1);
        double x2 = solutions.get(2);
        double y2 = solutions.get(3);
        double beta1 = solutions.get(4);
        double beta2 = solutions.get(5);
        double beta1_4 = solutions.get(6);
        double beta2_4 = solutions.get(7);
        Node s1 = new Node(x1, y1);
        Node s2 = new Node(x2, y2);
        double delta_beta1 = Math.abs(beta1 - beta1_4);
        double delta_beta2 = Math.abs(beta2 - beta2_4);
        return delta_beta1 < delta_beta2 ? beta1 : beta2;
    }

    private LinkedList<Double> simulate_scan(Node device, Node AP,
                                             double beta_local, int scan_time,
                                             boolean noise_flag) {
        double noise = noise_flag ? (new Random()).nextGaussian() * sigma : 0;
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
}
