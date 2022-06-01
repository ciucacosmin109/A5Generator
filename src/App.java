import javax.management.InvalidAttributeValueException;

public class App { 
    private static String getHex(byte[] arr){
        StringBuilder res = new StringBuilder();
        for (byte b : arr) {
            res.append(String.format("%02x ", b));
        }
        return res.toString();
    }
    private static long get64Bits(String str) throws InvalidAttributeValueException{
        if(str.length() != 8){
            throw new InvalidAttributeValueException();
        }

        long res = 0x0; // 64bits

        for (int i = 0; i < str.length(); i++) {
            int ascii = (int)str.charAt(i);

            res = res << 8; // make space for the next char
            res = res | ascii; // add the next char
        }  
        return res;
    }
 
    private static boolean doXOR(int register, int sizeInBits, int mask) {
        byte res = 0x0;
        for (int i = 0; i < sizeInBits; i++) { 
            if((mask & 1) != 0){
                res = (byte)((res & 1) ^ (register & 1));
            } 
            register >>>= 1;
            mask >>>= 1;
        } 
        return (res & 1) != 0;
    }
    private static int shiftA5(int register, int sizeInBits, int mask){
        // do XOR on the bits indicated by the mask
        boolean resB = doXOR(register, sizeInBits, mask);

        // shift the register
        register <<= 1;

        // add the new bit and return
        register |= (resB ? 1 : 0);
        return register;
    }
    public static byte[] A5Generator(String pass, int size) throws InvalidAttributeValueException{
        if(pass.length() != 8){
            throw new InvalidAttributeValueException();
        }

        byte[] result = new byte[size];
        int k = 0;

        // get the initial 64bits
        long bits = get64Bits(pass); 

        int X = 0x0, lenX = 19, cbX = 8, maskX = 1<<18 | 1<<17 | 1<<16 | 1<<13;
        int Y = 0x0, lenY = 22, cbY = 10, maskY = (1<<21 | 1<<20) << lenX;
        int Z = 0x0, lenZ = 23, cbZ = 10, maskZ = (1<<22 | 1<<21 | 1<<20 | 1<<7) << (lenX + lenY);

        long selMaskX = (1 << lenX) - 1;
        long selMaskY = (1 << lenY) - 1;
        long selMaskZ = (1 << lenZ) - 1;

        // Init registers
        X = (int)((bits >>> (lenY + lenZ)) & selMaskX);
        Y = (int)((bits >>> lenZ) & selMaskY);
        Z = (int)(bits & selMaskZ);

        // run the LFSR 'sizeInBits' times
        for (int i = 0; i < size + pass.length(); i++) { 
            for (int j = 0; j < 8; j++) {
                // Extract the result
                boolean rX = ((X >>> (lenX - 1)) & 1) != 0;
                boolean rY = ((Y >>> (lenY - 1)) & 1) != 0;
                boolean rZ = ((Z >>> (lenZ - 1)) & 1) != 0;
                boolean rr = ((rX?1:0) ^ (rY?1:0) ^ (rZ?1:0)) != 0;

                // Shift the registers that have the correct clocking bit
                boolean dominant = ((X >>> (cbX - 1)) & 1) + ((Y >>> (cbY - 1)) & 1) + ((Z >>> (cbZ - 1)) & 1) >= 2;
                if((((X >>> (cbX - 1)) & 1) != 0) == dominant){
                    X = shiftA5(X, lenX, maskX);
                }
                if((((Y >>> (cbY - 1)) & 1) != 0) == dominant){
                    Y = shiftA5(Y, lenY, maskY);
                }
                if((((Z >>> (cbZ - 1)) & 1) != 0) == dominant){
                    Z = shiftA5(Z, lenZ, maskZ);
                }

                // populate the result only if the algorithm has completed pass.length() iterations
                if(i >= pass.length()) {     
                    // make space for the next bit
                    result[k] = (byte)(result[k] << 1);
                    // add the value at the result
                    result[k] = (byte)(result[k] | (rr ? 1 : 0));
                } 
            }
            if(i >= pass.length()) {     
                k++;
            }  
        }

        return result;
    }
    
    private static int lastValue = 0;
    public static int[] getInts(byte[] input, int len) throws InvalidAttributeValueException{
        if(len % 4 != 0 || len > input.length){
            throw new InvalidAttributeValueException();
        }

        int[] result = new int[len / 4];
        for (int i = 0; i < len - 3; i += 4) {
            int a = 51567;
            int c = (input[i+1] << 8*3) | (input[i+3] << 8*2) | (input[i+2] << 8) | input[i];
            int m = 100;
    
            lastValue = (a * lastValue + c) % m;
            result[i / 4] = lastValue; 
        }

        return result;
    }

    public static void main(String[] args) throws Exception {
        String pass = "99999999";
 
        byte[] res1 = A5Generator(pass, 7);
        byte[] res2 = A5Generator(pass, 16);
        byte[] res3 = A5Generator(pass, 32);
        System.out.println("Sequence1: " + getHex(res1));
        System.out.println("Sequence2: " + getHex(res2));
        System.out.println("Sequence3: " + getHex(res3));
 
        int[] numbers = getInts(res3, 32);
        System.out.print("Random numbers: ");
        for (int i = 0; i < numbers.length; i++) {
            System.out.print(numbers[i] + " ");
        } 
    }
}
