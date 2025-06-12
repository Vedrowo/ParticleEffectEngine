import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class frame extends JFrame {
    private JTextField particleCountField, windowWidthField, windowHeightField, xField, yField, mode;
    private JPanel simulationPanel;
    private particleEngine particleEngine;
    private int windowWidth = 800;
    private int windowHeight = 600;
    private Timer particleTimer;
    private int particlesAdded = 0;


    public frame() {
        this.setTitle("Particle Simulation");
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Create and add the input panel
        JPanel inputPanel = createInputPanel();
        this.add(inputPanel, BorderLayout.NORTH);
// peepeepoopoo


        // Create simulation panel
        simulationPanel = new JPanel();
        simulationPanel.setPreferredSize(new Dimension(windowWidth, windowHeight));
        simulationPanel.setBackground(Color.BLACK);
        this.add(simulationPanel, BorderLayout.CENTER);

        this.pack();
        this.setLocationRelativeTo(null); // Center the window
        this.setVisible(true);
    }

    // Create the input panel with text fields and a button
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(7, 2, 5, 5)); // 5 rows, 2 columns

        // Add labels and text fields for inputs
        inputPanel.add(new JLabel("Number of Particles:"));
        particleCountField = new JTextField("1000");
        inputPanel.add(particleCountField);

        inputPanel.add(new JLabel("Window Width:"));
        windowWidthField = new JTextField(String.valueOf(windowWidth));
        inputPanel.add(windowWidthField);

        inputPanel.add(new JLabel("Window Height:"));
        windowHeightField = new JTextField(String.valueOf(windowHeight));
        inputPanel.add(windowHeightField);

        inputPanel.add(new JLabel("Starting X:"));
        xField = new JTextField("400"); // Default X position
        inputPanel.add(xField);

        inputPanel.add(new JLabel("Starting Y:"));
        yField = new JTextField("300"); // Default Y position
        inputPanel.add(yField);

        inputPanel.add(new JLabel("Mode: (burst / over time"));
        mode = new JTextField("over time");
        inputPanel.add(mode);


        // Add start button
        JButton startButton = new JButton("Start Simulation");
        inputPanel.add(startButton);

        // Action listener for the button
        startButton.addActionListener(e -> {
            try {
                int numParticles = Integer.parseInt(particleCountField.getText());
                windowWidth = Integer.parseInt(windowWidthField.getText());
                windowHeight = Integer.parseInt(windowHeightField.getText());
                int x = Integer.parseInt(xField.getText());
                int y = Integer.parseInt(yField.getText());
                String amode = mode.getText();

                // Update frame size
                this.setSize(windowWidth, windowHeight);
                //simulationPanel.setPreferredSize(new Dimension(windowWidth, windowHeight));

                // Initialize the particle engine if it's null
                if (particleEngine == null) {
                    particleEngine = new particleEngine(x, y, numParticles); // Pass x, y, and numParticles
                } else {
                    particleEngine.resetEngine(x, y, numParticles); // Reset the particle engine
                }

                if(Objects.equals(amode, "burst")){
                    for(int i = 0; i < numParticles; i++){
                        particleEngine.addParticles(x, y, windowWidth, windowHeight);
                    }
                }else{
                    particlesAdded = 0;
                    particleTimer = new Timer(10, e1 -> {
                        int particlesToAdd = 10; // Add 10 particles per tick
                        for (int i = 0; i < particlesToAdd && particlesAdded < numParticles; i++) {
                            particleEngine.addParticles(x, y, windowWidth, windowHeight);  // Add particle at (x, y)
                            particlesAdded++;
                        }
                        if (particleEngine.done()) {
                            particleTimer.stop();  // Stop the timer once all particles are added
                        }
                    });
                    particleTimer.start();
                }



                // Add a panel to visualize the simulation
                //simulationPanel = new simulationPanel(particleEngine);
                this.add(simulationPanel, BorderLayout.CENTER);
                this.revalidate();
                this.repaint();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numerical values.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return inputPanel;
    }
}
