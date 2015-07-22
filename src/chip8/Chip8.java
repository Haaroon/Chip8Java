 package chip8;

 import java.io.DataInputStream;
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.IOException;
 import java.util.Random;

 /**
  * Created by Haaroon on 14/07/2015.
  */
public class Chip8 {

    private char[] memory; //4096 size
    private char[] V; //8 16 bit registers
    private char I; //Address register, 16 bits wide
    private char pc; //program counter

    private char[] stack;
    private int stackPointer;

    private int delay_timer;
    private int sound_timer;

    private byte[] keys;
    private byte[] display;
    private boolean needRedraw;

    public void init() {
        memory = new char[4096];
        V = new char[16];
        I = 0x0; // set to zero in hex
        pc = 0x200; //first 512 bytes are reserved

        stack = new char[12];
        stackPointer = 0;

        delay_timer = sound_timer = 0;

        keys = new byte[16];
        display = new byte[ 64 * 32 ];

    }

    public void run(){
        //fetch opc
        char opcode = (char)((memory[pc] << 8) | memory[pc + 1]);
        //print what it got
        System.out.println("");
        System.out.print(Integer.toHexString(opcode) + " : ");
        char address, NN, NNN;
        int x, y;
        //decode opc
        switch (opcode & 0xF000 ){

            case 0x0000: //Multi-case
                switch(opcode & 0x00FF) {
                    case 0x00E0: //00E0: Clear Screen
                        for(int i = 0; i < display.length; i++)
                            display[i] = 0;
                        pc += 2;
                        needRedraw = true;
                        break;

                    case 0x00EE: //00EE: Returns from subroutine
                        stackPointer--;
                        pc = (char)(stack[stackPointer]+2);
                        System.out.print("Returning from subroutine "+ Integer.toHexString(pc));
                        break;

                    default: //0NNN: Calls RCA 1802 Program at address NNN
                        System.err.println("Unsupported Opcode efg!");
                        System.exit(0);
                        break;
                }
                break;

            /* Case 1NNN skip to NNN */
            case 0x1000:
                pc = (char)(opcode & 0x0FFF);
                System.out.print("Skipping to NNN");
                break;


        /* Case 2NNN call subroutine at NNN */
            case 0x2000:
                address = (char)(opcode & 0x0FFF);
                stack[stackPointer] = pc;
                stackPointer++;
                pc = address;
                System.out.print("Calling subroutine at NNN");
                break;

        /* Case 3xkk Skips the next instruction if VX equals NN */
            case 0x3000:
                NN = (char)(opcode & 0x00FF);
                System.out.println("Printing  nn ("+NN+")");
                System.out.println("Printing  nn char("+(char)NN+")");
                if(V[((opcode & 0x0F00) >> 8)] == NN) {
                    pc += 4;
                    System.out.print("Skipping as next instruction vx ("+V[(opcode & 0x0F00) >> 8]+") == nn ("+NN+")");
                }
                else {
                    pc += 2;
                    System.out.print("not skipping cause vx (" +
                            +V[(opcode & 0x0F00) >> 8]+") != nn ("+NN+")");
                }
                break;

            /* Skip next instruction if Vx != kk. */
            case 0x4000:
                NN = (char)(opcode & 0x00FF);
                x = (opcode & 0x0F00) >> 8;
                System.out.print("0x4000 checking vx and nn");
                if( V[x] != NN ) {
                    System.out.print(" - not equal, pc+2");
                    pc += 2;
                }
                else {
                    System.out.print(" - equal, pc+4");
                    pc += 4;
                }
                break;

            case 0x5000:
                NN = (char)(opcode & 0x00FF);
                x = (opcode & 0x0F00) >> 8;
                System.out.print("0x4000 checking vx and nn");
                if( V[x] == NN ) {
                    System.out.print(" - equal skipping, pc+4");
                    pc += 4;
                }
                else {
                    System.out.print(" - not equal, pc+2");
                    pc += 2;
                }
                break;

        /* Case 6XNN - put the value NN into register Vx */
            case 0x6000:
                int __x = (opcode & 0x0F00) >> 8;
                NN = (char)(opcode & 0x00FF);
                V[__x] = NN;
                pc += 2;
                System.out.print("putting nn into register vx");
                break;

        /* Case 7XNN - Adds NN to VX */
            case 0x7000:
                char kk = (char)(opcode & 0x00FF);
                V[(opcode & 0x0F00) >> 8] = (char)((V[(opcode & 0x0F00) >> 8] + kk) & 0xFF);
                pc += 2;
                System.out.print("Adding NN to VX");
                break;

        /* Case 8XYN - varies */
            case 0x8000:
                System.out.print("Case 0x8000");
                x = (opcode & 0x0F00) >> 8;
                y = (opcode & 0x00F0) >> 4;
                switch(opcode & 0x000F) {

                    /* Stores the value of register Vy in register Vx */
                    case 0x0000:
                        V[x] = V[y];
                        pc += 2;
                        System.out.print("0x800 stored Vy in Vx");
                        break;

                    /* Performs a bitwise OR on the values of Vx and Vy, then stores the result in Vx. */
                    case 0x0001:
                        V[x] = (char)(( V[x] | V[y] ) & 0xFF);
                        pc += 2;
                        System.out.print("0x800 OR'ed and stored");
                        break;

                    /* Performs a bitwise AND on the values of Vx and Vy, then stores the result in Vx.*/
                    case 0x0002:
                        V[x] = (char)( V[x] & V[y] );
                        pc += 2;
                        System.out.print("0x800 AND'ed and stored");
                        break;

                    /* Performs a bitwise exclusive OR on the values of Vx and Vy, then stores the result in Vx. */
                    case 0x0003:
                        V[x] = (char)((V[x] ^ V[y]) & 0xFF);
                        pc += 2;
                        System.out.print("0x800 EXOR'ed and stored");
                        break;

                    /* The values of Vx and Vy are added together. If the result is greater than 8 bits (i.e., > 255,)
                       VF is set to 1, otherwise 0. Only the lowest 8 bits of the result are kept, and stored in Vx */
                    case 0x0004:
                        if(V[y] > 0xFF - V[x]) {
                            V[0xF] = 1;
                            System.out.println("Carry!");
                        } else {
                            V[0xF] = 0;
                            System.out.println("No Carry");
                        }
                        V[x] = (char)((V[x] + V[y]) & 0xFF);
                        pc += 2;
                        System.out.print("0x800 ADDED and changed");
                        break;

                    /* Set Vx = Vx - Vy, set VF = NOT borrow
                     If Vx > Vy, then VF is set to 1, otherwise 0. Then Vy is subtracted from Vx, and the results stored
                     in Vx. */
                    case 0x0005:
                        if(V[x] > V[y]) {
                            V[0xF] = 1;
                            System.out.println("No Borrow");
                        } else {
                            V[0xF] = 0;
                            System.out.println("Borrow");
                        }
                        V[x] = (char)((V[x] - V[y]) & 0xFF);
                        pc += 2;
                        System.out.print("0x800 comparing vx setting");
                        break;

                    /* If the least-significant bit of Vx is 1, then VF is set to 1, otherwise 0. Then Vx is divided by 2. */
                    case 0x0006:
                        V[0xF] = (char)(V[x] & 0x1);
                        V[x] = (char)(V[x] >> 1);
                        pc += 2;
                        System.out.println("0x800 Shifting Vx");
                        break;

                    /* Set Vx = Vy - Vx, set VF = NOT borrow.
                        If Vy > Vx, then VF is set to 1, otherwise 0. Then Vx is subtracted from Vy, and the results
                        stored in Vx.*/
                    case 0x0007:
                        if(V[x] > V[y])
                            V[0xF] = 0;
                        else
                            V[0xF] = 1;
                        V[x] = (char)((V[y] - V[x]) & 0xFF);
                        pc += 2;
                        System.out.println("0x800 minusing");
                        break;

                    /* If the most-significant bit of Vx is 1, then VF is set to 1, otherwise to 0. Then Vx is
                       multiplied by 2 */
                    case 0x000E:
                        V[0xF] = (char)(V[x] & 0x80);
                        V[x] = (char)(V[x] << 1);
                        pc += 2;
                        System.out.println("0x800 setting Vx");
                        break;

                    /* Crash and cry */
                    default:
                        System.err.println("Unsupported OPC 0x8");
                        System.exit(0);
                        break;
                }
        /* Skips the next instruction if VX doesn't equal VY */
            case 0x9000:
                x = (opcode & 0x0F00) >> 8;
                y = (opcode & 0x00F0) >> 4;
                if(V[x] != V[y]) {
                    pc += 4;
                } else {
                    pc += 2;
                }
                break;

        /* Case ANNN - The value of register I is set to NNN */
            case 0xA000:
                I = (char)(opcode & 0x0FFF);
                pc += 2;
                System.out.print("setting value of i to nnn");
                break;
        /* Jumps to the address NNN plus V0. */
            case 0xB000: {
                int nnn = opcode & 0x0FFF;
                int ex  = V[0] & 0xFF;
                pc = (char)(nnn + ex);
                break;
            }
        /* Case Cxkk - Sets VX to a random number, masked by NN */
            case 0xC000:
                x = (opcode & 0x0F00) >> 8;
                Random rand = new Random();
                int rannum = rand.nextInt(255);
                int nn = (opcode & 0x00FF);
                V[x] = (char)(rannum & nn);
                pc += 2;
                System.out.print("setting v["+x+"] to a random value ("+rannum+")");
                break;

        /* Case  DNYN -
        Sprites stored in memory at location in index  register (I) maximum 8bits wide. Wraps around the screen. If
        when drawn, clears a pixel, register VF is set to 1 o
        therwise it is zero. All drawing is XOR drawing */
            case 0xD000: {
                System.out.print("drawing on screen");
                x = V[(opcode & 0x0F00) >> 8];
                y = V[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;

                V[0xF] = 0;

                for (int _y = 0; _y < height; _y++) {
                   int line = memory[I + _y];
                    for (int _x = 0; _x < 8; _x++) {
                        int pixel = line & (0x80 >> _x);
                        if (pixel != 0) {
                            int totalX = x + _x;
                            int totalY = y + _y;
                            int index = totalY * 64 + totalX;

                            if (display[index] == 1)
                                V[0xF] = 1;

                            display[index] ^= 1;
                        }
                    }
                }
                pc += 2;
                needRedraw = true;
                break;
            }

            case 0xE000: {
                switch (opcode & 0x00FF) {
                    case 0x009E: { //EX9E Skip the next instruction if the Key VX is pressed
                        x = (opcode & 0x0F00) >> 8;
                        int key = V[x];
                        if(keys[key] == 1) {
                            pc += 4;
                        } else {
                            pc += 2;
                        }
                        System.out.println("Skipping next instruction if V[" + x + "] = " + ((int)V[x])+ " is pressed");
                        break;
                    }

                    case 0x00A1: { //EXA1 Skip the next instruction if the Key VX is NOT pressed
                        x = (opcode & 0x0F00) >> 8;
                        int key = V[x];
                        if(keys[key] == 0) {
                            pc += 4;
                        } else {
                            pc += 2;
                        }
                        break;
                    }

                    default:
                        System.err.println("Unexisting opcode");
                        System.exit(0);
                        return;
                }
                break;
            }

            case 0xF000: {
                switch(opcode & 0x00FF) {
                    case 0x0007: //Sets VX to the value of the delay timer.
                        x = (opcode & 0x0F00) >> 8;
                        V[x] = (char)delay_timer;
                        pc += 2;
                        System.out.print("Sets the vx to delay timer");
                        break;

                    case 0x000A: //A key press is awaited, and then stored in VX
                        x = (opcode & 0x0F00) >> 8;
                        for(int i = 0; i < keys.length; i++) {
                            if(keys[i] == 1) {
                                V[x] = (char)i;
                                pc += 2;
                                break;
                            }
                        }
                        break;

                    case 0x0015: //Sets the delay timer to VX
                        x = (opcode & 0x0F00) >> 8;
                        delay_timer = V[x];
                        pc += 2;
                        System.out.print("Sets the delay timer to VX");
                        break;

                    case 0x0018: //Sets the sound timer to VX.
                        x = (opcode & 0x0F00) >> 8;
                        sound_timer = V[x];
                        pc += 2;
                        break;

                    case 0x001E: //Adds VX to I
                        x = (opcode & 0x0F00) >> 8;
                        I = (char)(I + V[x]);
                        System.out.println("Adding V[" + x + "] = " + (int)V[x] + " to I");
                        pc += 2;
                        break;

                    case 0x0029: //Sets I to the location of the sprite for the character in VX.
                                 // Characters 0-F (in hexadecimal) are represented by a 4x5 font
                        x = (opcode & 0x0F00) >> 8;
                        int character = V[x];
                        I = (char)(0x050 + (character * 5));
                        pc += 2;
                        break;

                    case 0x0033: //Stores the Binary-coded decimal representation of VX, with the
                                // most significant of three digits at the address in I, the middle
                                // digit at I plus 1, and the least significant digit at I plus 2.
                                // (In other words, take the decimal representation of VX, place the
                                // hundreds digit in memory at location in I, the tens digit at
                                // location I+1, and the ones digit at location I+2.)
                        x = (opcode & 0x0F00) >> 8;
                        int value = V[x];
                        int hundreds = (value - (value % 100)) / 100;
                        value -= hundreds * 100;
                        int tens = (value - (value % 10))/ 10;
                        value -= tens * 10;
                        memory[I] = (char)hundreds;
                        memory[I + 1] = (char)tens;
                        memory[I + 2] = (char)value;
                        System.out.println("adding values to memory");
                        pc += 2;
                        break;

                    case 0x0055: //Stores V0 to VX in memory starting at address I
                        x = (opcode & 0x0F00) >> 8;
                        for(int i = 0; i <= x; i++) {
                            memory[I + i] = V[i];
                        }
                        System.out.println("Setting Memory[" + Integer.toHexString(I & 0xFFFF).toUpperCase() + " + n] = V[0] to V[x]");
                        pc += 2;
                        break;

                    case 0x0065: //Fills V0 to VX with values from memory starting at address I
                        x = (opcode & 0x0F00) >> 8; //get he value of x
                        for(int i = 0; i <= x; i++) {
                            V[i] = memory[I + i];
                        }
                        I = (char)(I + x + 1);
                        pc += 2;
                        System.out.print("filling values of v0-vx from i");
                        break;

                    default: //0NNN: Calls RCA 1802 Program at address NNN
                        System.err.println("Unsupported Opcode in 0xF!");
                        System.exit(0);
                        break;
                }
                break;
            }

        default:
            System.out.println("Unsupported OPC def");
            System.exit(0);
        }

        if(sound_timer > 0)
            sound_timer--;

        if(delay_timer > 0)
            delay_timer--;
        //do/exec opcode
    }

     public byte[] getDisplay() {
         return display;
     }

     public boolean needsRedraw() {
         return needRedraw;
     }

     public void removeDrawFlag() {
         needRedraw = false;
     }

     public void loadProgram(String file) {
         DataInputStream input = null;
         try {
             input = new DataInputStream(new FileInputStream(new File(file)));

             int offset = 0;
             while(input.available() > 0) {
                 memory[0x200 + offset] = (char)(input.readByte() & 0xFF);
                 offset++;
             }

         } catch (IOException e) {
             e.printStackTrace();
             System.exit(0);
         } finally {
             if(input != null) {
                 try { input.close(); } catch (IOException ex) {}
             }
         }
     }

     public void setKeyBuffer(int[] keyBuffer) {
         for(int i = 0; i < keys.length; i++) {
             keys[i] = (byte)keyBuffer[i];
         }
     }
}
