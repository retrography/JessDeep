/**
 * Created by Mah on 2015-4-24.
 */

public class App {

    public static void main(String[] args) {

/*
        Scanner scan = new Scanner(System.in);
        String s1 = scan.next();
        String s2 = scan.next();
*/


        String s1 = "96:XaYjV6257bsanRv3U0UgFKeUdPTU/aTeUp906KF76b/UAUwCz6jFt6U6H4UPFYFW:Xa2kanyim/6uOQSOV8Q6uNzu6Z6";
        String s2 = "96:xaYjV6257bsanRv3U0UgFKeUdPTU/aTeUp906KF76b/UAUwCz6jFt6U6H4UPFYFI:xa2kanyim/6uOQSOV8Q6uNhu6Z6";

        SpamSumSignature sss1 = new SpamSumSignature(s1);
        SpamSumSignature sss2 = new SpamSumSignature(s2);

        Ssdeep ssd = new Ssdeep();
        int score = ssd.Compare(sss1, sss2);

        System.out.println(score);

/*

        RUN_TEST("", "Hello world", 12, "Empty source");
        RUN_TEST("Hello world", "", 12, "Empty destination");
        RUN_TEST("Hello world", "Hello world", 0, "Equal strings");
        RUN_TEST("Hello world", "Hell world", 1, "Delete");
        RUN_TEST("Hell world", "Hello world", 1, "Insert");
        RUN_TEST("Hello world", "Hello owrld", 2, "Swap");
        RUN_TEST("Hello world", "HellX world", 2, "Change");


*/

    }

    public static void RUN_TEST(String s3, String s4, int out, String msg) {

//        int dist1 = EditDistance.edit_distn(s3.getBytes(), s3.length(), s4.getBytes(), s4.length());
        int dist2 = EditDistance.edit_distance(s3, s4);

        System.out.println(msg);

//        System.out.print("Old: ");
//        System.out.println(dist1 == out);
//        System.out.print("New: ");
        System.out.print(dist2 == out);
        System.out.print(": ");
        System.out.print(out);
        System.out.print(" vs ");
        System.out.println(dist2);


    }
}