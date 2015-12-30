import java.util.Scanner;

/**
 * Created by Mah on 2015-4-24.
 */

public class App {

    public static void main(String[] args) {


        Scanner scan = new Scanner(System.in);
        System.out.print("Signatures: ");
        String s = scan.next();
        String[] ss = s.split(",");


        String s1 = ss[0];
        String s2 = ss[1];


        int score = SpamSumSignature.fuzzyCompare(s1, s2);

        System.out.println(score);


    }


}