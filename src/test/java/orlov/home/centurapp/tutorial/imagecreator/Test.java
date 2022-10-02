package orlov.home.centurapp.tutorial.imagecreator;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Test extends JPanel {

    public void paint(Graphics g) {
        Image img = createImageWithText();
        g.drawImage(img, 20,20,this);
    }

    private Image createImageWithText() {
        BufferedImage bufferedImage = new BufferedImage(200,120,BufferedImage.TYPE_INT_RGB);
        Graphics g = bufferedImage.getGraphics();

        g.drawString("www.tutorialspoint.com", 20,20);
        g.drawString("www.tutorialspoint.com", 20,40);
        g.drawString("www.tutorialspoint.com", 20,60);
        g.drawString("www.tutorialspoint.com", 20,80);
        g.drawString("www.tutorialspoint.com", 20,100);

        return bufferedImage;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new Test());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200, 200);
        frame.setVisible(true);
    }

}
