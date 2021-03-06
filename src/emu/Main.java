package emu;

import chip8.Chip8;

public class Main extends Thread {

    private Chip8 chip8;
    private Chip8Frame frame;

    public Main() {
        chip8 = new Chip8();
        chip8.init();
        chip8.loadProgram("./pong2.c8");
        frame = new Chip8Frame(chip8);
    }

    public void run() {
        //60 hz, 60 updates per second
        while(true) {
            chip8.run();
            if(chip8.needsRedraw()) {
                frame.repaint();
                chip8.removeDrawFlag();
            }
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                //Unthrown exception
            }
        }
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }

}