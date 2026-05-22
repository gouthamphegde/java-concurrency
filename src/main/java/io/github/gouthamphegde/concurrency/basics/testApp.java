    import java.util.stream.IntStream;

    public class testApp {
        static int factorial(int n) {
                return IntStream.rangeClosed(1, n).reduce(1, Integer::multiply);
            }
        public static void main(String[] args) {

        }
    }
}
