import com.sun.j3d.utils.applet.JMainFrame;

import javax.media.j3d.BadTransformException;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.EmptyStackException;
import java.util.Stack;

public class CograGUI extends JApplet {

    private static final int DIMENSION = 3;

    private static final int SLIDE_START = 0;
    private static final int SLIDE_END = 360;
    private static final int MINOR_TICK = SLIDE_END / 36;
    private static final int MAJOR_TICK = SLIDE_END / 4;

    private static final String TRANSFORMATION_MATRIX_PANEL_NAME = "Manually";
    private static final String EASY_TRANSFORM_PANEL_NAME = "Step by step";

    private final Canvas3Dim canvas;
    private final JTextField[] transformMatrixTextFields
        = new JTextField[(DIMENSION + 1) * (DIMENSION + 1)];
    private final Stack<double[]> undoStack = new Stack<double[]>();
    private final Stack<double[]> redoStack = new Stack<double[]>();
    private final JButton undoBtn = new JButton("Undo");
    private final JButton redoBtn = new JButton("Redo");
    private final JButton clearBtn = new JButton("Clear");
    private final double[] defaultMatrixTextFieldValues
        = MatrixOperations.matrixToArray(MatrixOperations.getIdentityMatrix());
    private final JButton startBtn = new JButton("Transform");
    private final JLabel easyRotateLabel = new JLabel("Rotate");
    private final JLabel easyTranslateLabel = new JLabel("Translate");
    private final JLabel easyScaleLabel = new JLabel("Scale");
    private final JTextField easyRotXTextField = new JTextField();
    private final JTextField easyRotYTextField = new JTextField();
    private final JTextField easyRotZTextField = new JTextField();
    private final JTextField easyTranslXTextField = new JTextField();
    private final JTextField easyTranslYTextField = new JTextField();
    private final JTextField easyTranslZTextField = new JTextField();
    private final JTextField easyScaleXTextField = new JTextField();
    private final JTextField easyScaleYTextField = new JTextField();
    private final JTextField easyScaleZTextField = new JTextField();
    // eto tozhe vse tupo, no ne pisat zhe dlia etogo zikl?!
    private final JComponent[] easyTransformComponents = new JComponent[] {
        easyRotateLabel, new JLabel(),
        easyRotXTextField, new JLabel("° around x-axis"),
        easyRotYTextField, new JLabel("° around y-axis"),
        easyRotZTextField, new JLabel("° around z-axis"),
        easyTranslateLabel, new JLabel(),
        easyTranslXTextField, new JLabel(" along x-axis"),
        easyTranslYTextField, new JLabel(" along y-axis"),
        easyTranslZTextField, new JLabel(" along z-axis"),
        easyScaleLabel, new JLabel(),
        easyScaleXTextField, new JLabel(" along x-axis"),
        easyScaleYTextField, new JLabel(" along y-axis"),
        easyScaleZTextField, new JLabel(" along z-axis")
    };
    private final JSlider xSlider = new JSlider(JSlider.HORIZONTAL, SLIDE_START, SLIDE_END, SLIDE_START);
    private final JSlider ySlider = new JSlider(JSlider.HORIZONTAL, SLIDE_START, SLIDE_END, SLIDE_START);
    private final JSlider zSlider = new JSlider(JSlider.HORIZONTAL, SLIDE_START, SLIDE_END, SLIDE_START);

    public static void main(String[] args) {
        JFrame frame = new JMainFrame(new CograGUI(), 500, 500);
        frame.setTitle("Cogra");
        frame.pack();
    }

    public CograGUI() {
        GridBagLayout gbl = new GridBagLayout();
        setLayout(gbl);
        GridBagConstraints constraints = new GridBagConstraints();
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        JPanel transformationMatrixPanel = new JPanel(new GridLayout(DIMENSION + 1, DIMENSION + 1));
        for (int k = 0; k < (DIMENSION + 1) * (DIMENSION + 1); k++) {
            transformMatrixTextFields[k] =
                new JTextField(writeToField(defaultMatrixTextFieldValues[k]), 6);
            transformMatrixTextFields[k].setHorizontalAlignment(JTextField.CENTER);
            transformationMatrixPanel.add(transformMatrixTextFields[k]);
            transformMatrixTextFields[k].addKeyListener(new MatrixTransformStartListener());
            transformMatrixTextFields[k].addFocusListener(new TextSelector());
        }

        JPanel startPan = new JPanel();
        startBtn.addActionListener(new MatrixTransformStartListener());
        startPan.add(startBtn);

        tabbedPane.addTab(TRANSFORMATION_MATRIX_PANEL_NAME, transformationMatrixPanel);
        JPanel easyTransformPanel = new JPanel(new GridLayout(12, 2));
        setDefaultEasyTextFieldValues();
        tabbedPane.addTab(EASY_TRANSFORM_PANEL_NAME, easyTransformPanel);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JTabbedPane source = (JTabbedPane) e.getSource();
                for (ActionListener al : startBtn.getActionListeners()) {
                    startBtn.removeActionListener(al);
                }
                if (source.getTitleAt(source.getSelectedIndex()).equals(TRANSFORMATION_MATRIX_PANEL_NAME)) {
                    startBtn.addActionListener(new MatrixTransformStartListener());
                } else if (source.getTitleAt(source.getSelectedIndex()).equals(EASY_TRANSFORM_PANEL_NAME)) {
                    startBtn.addActionListener(new EasyTransformStartListener());
                }
            }
        });

        for (JComponent component : easyTransformComponents) {
            easyTransformPanel.add(component);
            if (component instanceof JTextField) {
                component.addFocusListener(new TextSelector());
                component.addKeyListener(new EasyTransformStartListener());
            }
        }

        canvas = new Canvas3Dim(getMatrixTextFieldValues());
        JPanel undoRedoPan = new JPanel(new FlowLayout());
        undoRedoPan.add(undoBtn);
        undoRedoPan.add(redoBtn);
        undoBtn.addActionListener(new UndoRedoListener());
        redoBtn.addActionListener(new UndoRedoListener());
        setButtonEnablities();
        undoRedoPan.add(clearBtn);
        clearBtn.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    while (!undoStack.isEmpty()) {
                        transformViaUndoRedoBtns(true, true);
                    }
                    setMatrixTextFieldValues(defaultMatrixTextFieldValues);
                    setDefaultEasyTextFieldValues();
                }
            }
        );

        JPanel slidePan = new JPanel(new GridLayout(6, 1));
        JSlider[] sliders = new JSlider[] {
            xSlider, ySlider, zSlider
        };
        char axis = 'X';
        for (JSlider slider : sliders) {
            slidePan.add(new JLabel("Rotate around " + axis++ + "-axis"));
            slidePan.add(slider);
            slider.addChangeListener(new SliderChangeListener());
            slider.setMajorTickSpacing(MAJOR_TICK);
            slider.setMinorTickSpacing(MINOR_TICK);
            slider.setPaintLabels(true);
            slider.setPaintTicks(true);
            slider.setPaintTrack(true);
        }


        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 4, 0, 4);
        JLabel transformationLabel = new JLabel("Transform the object");
        gbl.setConstraints(transformationLabel, constraints);
        add(transformationLabel);
        constraints.gridheight = GridBagConstraints.REMAINDER;
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.anchor = GridBagConstraints.NORTH;
        gbl.setConstraints(canvas.getCanvas3D(), constraints);
        add(canvas.getCanvas3D());
        constraints.insets = new Insets(4, 4, 0, 4);
        constraints.gridheight = 1;
        constraints.gridx = GridBagConstraints.REMAINDER;
        gbl.setConstraints(tabbedPane, constraints);
        add(tabbedPane);
        gbl.setConstraints(startPan, constraints);
        add(startPan);
        gbl.setConstraints(undoRedoPan, constraints);
        add(undoRedoPan);
        constraints.anchor = GridBagConstraints.SOUTH;
        constraints.insets = new Insets(0, 0, 20, 0);
        gbl.setConstraints(slidePan, constraints);
        add(slidePan);
    }

    private void transform(double[] values, boolean isFast) {
        MatrixOperations.makeAffine(values);
        canvas.matrixTransformObject(values, isFast);
    }

    private static String writeToField(double n) {
        return Double.toString(n);
    }

    public void showDialog(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public double[] getMatrixTextFieldValues() {
        double[] elements = new double[transformMatrixTextFields.length];
        int i = 0;
        for (JTextField tf : transformMatrixTextFields) {
            elements[i++] = Double.parseDouble(tf.getText());
        }
        return elements;
    }

    private void setButtonEnablities() {
        boolean isRedoStackEmpty = redoStack.isEmpty();
        boolean isInvertible = MatrixOperations.isInvertible(getMatrixTextFieldValues())
            && !undoStack.isEmpty();
        undoBtn.setEnabled(isInvertible);
        redoBtn.setEnabled(!isRedoStackEmpty);
        clearBtn.setEnabled(isInvertible);
    }

    private void transformViaUndoRedoBtns(boolean isUndo, boolean isFast) {
        Stack<double[]> thisStack = isUndo ? undoStack : redoStack;
        Stack<double[]> otherStack = isUndo ? redoStack : undoStack;
        try {
            double[] values = thisStack.pop();
            double[] transformValues = isUndo ?
                MatrixOperations.matrixToArray(MatrixOperations.invert(values)) : values;
            otherStack.push(values);
            transform(transformValues, isFast);
            setButtonEnablities();
        } catch (EmptyStackException ignored) { // will never occur
        }
    }

    private void setDefaultEasyTextFieldValues() {
        for (JComponent comp : easyTransformComponents) {
            if (comp instanceof JTextField) {
                if (comp == easyScaleXTextField ||
                    comp == easyScaleYTextField ||
                    comp == easyScaleZTextField) {
                    ((JTextComponent) comp).setText("1.0");
                } else {
                    ((JTextComponent) comp).setText("0.0");
                }
            }
        }
    }

    private void setMatrixTextFieldValues(double[] values) {
        for (int k = 0; k < transformMatrixTextFields.length; k++) {
            transformMatrixTextFields[k].setText(Double.toString(values[k]));
        }
    }

    private class UndoRedoListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            transformViaUndoRedoBtns(e.getSource() == undoBtn, false);
        }

    }

    private abstract class StartListener implements KeyListener, ActionListener {

        double[] transformationValues;
        int currCol;
        int currRow;

        public void keyTyped(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
        }

        public abstract void starty();

        public abstract String getNumberFormatErrorText();

        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                start();
            }
        }

        public void actionPerformed(ActionEvent e) {
            start();
        }

        public void start() {
            try {
                starty();
            } catch (NumberFormatException ex) {
                showDialog(getNumberFormatErrorText());
            }
            organizeStacks();
            setButtonEnablities();
        }

        private void organizeStacks() {
            undoStack.push(transformationValues);
            redoStack.clear();
        }

    }

    private static class TextSelector implements FocusListener {

        public void focusLost(FocusEvent e) {
        }

        public void focusGained(FocusEvent e) {
            ((JTextComponent) e.getSource()).selectAll();
        }

    }

    private class MatrixTransformStartListener extends StartListener {

        public void starty() {
            int mlength = transformMatrixTextFields.length;
            transformationValues = new double[mlength];
            for (int i = 0; i < mlength; i++) {
                String text = transformMatrixTextFields[i].getText();
                if (text.isEmpty()) {
                    text = "0";
                    transformMatrixTextFields[i].setText(text);
                }
                currRow = i / (DIMENSION + 1) + 1;
                currCol = i % (DIMENSION + 1) + 1;
                transformationValues[i] = MatrixOperations.getMatrixElementValue(text);
            }
            try {
                transform(transformationValues, false);
            } catch (BadTransformException bte) {
                showDialog("Non-affine transform.");
            }
        }

        public String getNumberFormatErrorText() {
            return "Matrix element (" + currRow + ", " + currCol +
                ") has an inappropriate format.";
        }

    }

    private class EasyTransformStartListener extends StartListener {

        public void starty() {
            currRow = 0;
            double[] values = new double[9];
            for (JComponent component : easyTransformComponents) {
                if (component instanceof JTextField) {
                    String text = ((JTextComponent) component).getText();
                    if (text.isEmpty()) {
                        text = "0";
                        ((JTextComponent) component).setText(text);
                    }
                    values[currRow++] = MatrixOperations.getMatrixElementValue(text);
                }
            }
            double rotX = values[0];
            double rotY = values[1];
            double rotZ = values[2];
            double translX = values[3];
            double translY = values[4];
            double translZ = values[5];
            double scaleX = values[6];
            double scaleY = values[7];
            double scaleZ = values[8];
            // eto ochen tupo? Mozhno bez etogo, no togda budet ploho chitaemo
            transformationValues = canvas.manualTransformObject(rotX, rotY, rotZ, translX, translY, translZ, scaleX, scaleY, scaleZ, false);
            setButtonEnablities();
        }

        public String getNumberFormatErrorText() {
            int col = currRow - 1;
            String wrongRow;
            int axis = col % 3;
            if (axis == 0) {
                wrongRow = "X";
            } else if (axis == 1) {
                wrongRow = "Y";
            } else {
                wrongRow = "Z";
            }
            if (col < 3) {
                wrongRow += "-angle";
            } else if (col > 2 && col < 6) {
                wrongRow += "-translation value";
            } else {
                wrongRow += "-scaling value";
            }
            return wrongRow + " has an inappropriate format.";
        }

    }

    private class SliderChangeListener implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            int value = source.getValue();
            if (source == xSlider) {
                canvas.rotateX(value);
            } else if (source == ySlider) {
                canvas.rotateY(value);
            } else if (source == zSlider) {
                canvas.rotateZ(value);
            }
        }
    }
}
