import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.geometry.Box;

import javax.media.j3d.*;
import javax.vecmath.*;
import java.awt.*;

public class Canvas3Dim {

    private static final double TO_RADIANS = Math.PI / 180d;

    private final Canvas3D canvas3D;
    private final TransformGroup transformGroup = new TransformGroup();
    private final TransformGroup sceneTransformGroup = new TransformGroup();
    private final Matrix4d oldMatrix;
    private double xAngle = 0d;
    private double yAngle = 0d;
    private double zAngle = 0d;
    private final Matrix4d axisMatrix = MatrixOperations.getIdentityMatrix();

    public Canvas3Dim(double[] oldMatrixValues) {
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        canvas3D = new Canvas3D(config);
        canvas3D.setSize(1000, 800);
        oldMatrix = new Matrix4d(oldMatrixValues);
        Background background = new Background();
        background.setColor(0f, 0f, 0f);
        BoundingSphere worldBounds = new BoundingSphere(new Point3d(.0, .0, .0), 3000.0);
        background.setApplicationBounds(worldBounds);

        AmbientLight lightA = new AmbientLight();
        lightA.setInfluencingBounds(worldBounds);
        DirectionalLight lightD = new DirectionalLight();
        lightD.setInfluencingBounds(worldBounds);

        Transform3D sceneTransform3D = new Transform3D();
        Transform3D tempTransform3D = new Transform3D();
        tempTransform3D.rotY(-Math.PI / 4d);
        sceneTransform3D.rotX(Math.PI / 4d);
        sceneTransform3D.mul(tempTransform3D);
        tempTransform3D.setIdentity();
        tempTransform3D.setTranslation(new Vector3d(-10d, -15d, -10d));
        sceneTransform3D.mul(tempTransform3D);
        sceneTransformGroup.setTransform(sceneTransform3D);
        sceneTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        BranchGroup objRoot = new BranchGroup();
        createSceneGraph(objRoot);
        sceneTransformGroup.addChild(objRoot);
        BranchGroup scene = new BranchGroup();
        scene.addChild(sceneTransformGroup);
        scene.addChild(background);
        scene.addChild(lightA);
        scene.addChild(lightD);
        scene.compile();

        SimpleUniverse simpleU = new SimpleUniverse(canvas3D);
        simpleU.getViewingPlatform().setNominalViewingTransform();
        simpleU.addBranchGraph(scene);
    }

    private void createSceneGraph(BranchGroup objRoot) {
        createBody(objRoot);
        createAxes(objRoot);
    }

    public Canvas3D getCanvas3D() {
        return canvas3D;
    }

    public void matrixTransformObject(double[] newValues, boolean isFast) {
        Matrix4d newMatrix = new Matrix4d(newValues);
        Matrix4d tempMatrix = new Matrix4d(oldMatrix);
        Matrix4d delta = new Matrix4d();
        Transform3D transform = new Transform3D();
        transformGroup.getTransform(transform);
        int sleepTime = isFast ? 1 : 3;
        int repeats = 400;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                delta.setElement(i, j, (newMatrix.getElement(i, j) - oldMatrix.getElement(i, j)) / repeats);
            }
        }

        for (int k = 0; k < repeats; k++) {
            try {
                Thread.sleep(sleepTime);
                tempMatrix.add(delta);

                tempMatrix.transpose();
                Transform3D tempTransform = new Transform3D(tempMatrix);
                tempTransform.mul(transform);
                tempMatrix.transpose();

                transformGroup.setTransform(tempTransform);
            } catch (InterruptedException ignored) {
                //
            }
        }
    }

    public double[] manualTransformObject(
        double rotX, double rotY, double rotZ,
        double transX, double transY, double transZ,
        double scaleX, double scaleY, double scaleZ,
        boolean isFast) {
        Transform3D xRotT3D = new Transform3D();
        Transform3D yRotT3D = new Transform3D();
        Transform3D zRotT3D = new Transform3D();
        Transform3D translateT3D = new Transform3D();
        Transform3D scaleT3D = new Transform3D();
        Transform3D[] t3ds = new Transform3D[] {
            xRotT3D, yRotT3D, zRotT3D, translateT3D, scaleT3D
        };
        Transform3D transform = new Transform3D();
        xRotT3D.rotX(rotX * TO_RADIANS);
        yRotT3D.rotY(rotY * TO_RADIANS);
        zRotT3D.rotZ(rotZ * TO_RADIANS);
        translateT3D.setTranslation(new Vector3d(transX, transY, transZ));
        scaleT3D.setScale(new Vector3d(scaleX, scaleY, scaleZ));
        for (Transform3D t : t3ds) {
            transform.mul(t);
        }
        Matrix4d matrix = new Matrix4d();
        transform.get(matrix);
        matrix.transpose();
        double[] easyTransformMatrix = MatrixOperations.matrixToArray(matrix);
        matrixTransformObject(easyTransformMatrix, isFast);
        return easyTransformMatrix;
    }

    private void createBody(BranchGroup objRoot) {
        Appearance appearance = new Appearance();
        appearance.setColoringAttributes(new ColoringAttributes(.0f, .0f, .0f, ColoringAttributes.SHADE_FLAT));
        TransparencyAttributes ta = new TransparencyAttributes();
        ta.setTransparencyMode(TransparencyAttributes.FASTEST);
        ta.setTransparency(0f);
        appearance.setTransparencyAttributes(ta);
        appearance.setMaterial(new Material());
        Box box = new Box(1f, 1f, 1f, Box.GENERATE_NORMALS, appearance);
        Transform3D initialRotate = new Transform3D();
        transformGroup.setTransform(initialRotate);
        transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        transformGroup.addChild(box);
        objRoot.addChild(transformGroup);
    }

    private static void createAxes(BranchGroup objRoot) {
        BranchGroup axes = new BranchGroup();
        Appearance axesAppearance = new Appearance();
        Color3f axesColor = new Color3f(1f, 1f, 1f);
        ColoringAttributes coloringAttributes = new ColoringAttributes();
        coloringAttributes.setColor(axesColor);
        axesAppearance.setColoringAttributes(coloringAttributes);
        Point3d origin = new Point3d(0d, 0d, 0d);
        Point3d[] vertexes = new Point3d[] {
            origin, new Point3d(100d, 0d, 0d),
            origin, new Point3d(0d, 100d, 0d),
            origin, new Point3d(0d, 0d, 100d)
        };
        LineArray lineArray = new LineArray(vertexes.length, LineArray.COORDINATES);
        lineArray.setCoordinates(0, vertexes);
        axes.addChild(new Shape3D(lineArray, axesAppearance));
        objRoot.addChild(axes);
    }

    public void rotateX(int x) {
        doRotateScene(x * TO_RADIANS, yAngle, zAngle);
    }

    public void rotateY(int y) {
        doRotateScene(xAngle, y * TO_RADIANS, zAngle);
    }

    public void rotateZ(int z) {
        doRotateScene(xAngle, yAngle, z * TO_RADIANS);
    }

    private void doRotateScene(double x, double y, double z) {
        double deltaX = x - xAngle;
        double deltaY = y - yAngle;
        double deltaZ = z  - zAngle;
        xAngle = x;
        yAngle = y;
        zAngle = z;
        Transform3D t3dRotX = new Transform3D();
        Transform3D t3dRotY = new Transform3D();
        Transform3D t3dRotZ = new Transform3D();
        Vector3d xAxis = new Vector3d(axisMatrix.m00, axisMatrix.m10, axisMatrix.m20);
        Vector3d yAxis = new Vector3d(axisMatrix.m01, axisMatrix.m11, axisMatrix.m21);
        Vector3d zAxis = new Vector3d(axisMatrix.m02, axisMatrix.m12, axisMatrix.m22);
        t3dRotX.setRotation(new AxisAngle4d(xAxis, deltaX));
        t3dRotY.setRotation(new AxisAngle4d(yAxis, deltaY));
        t3dRotZ.setRotation(new AxisAngle4d(zAxis, deltaZ));

        Transform3D t3d = new Transform3D();
        sceneTransformGroup.getTransform(t3d);

        t3d.mul(t3dRotX);
        t3d.mul(t3dRotY);
        t3d.mul(t3dRotZ);
        sceneTransformGroup.setTransform(t3d);

        Transform3D temp3d = new Transform3D();
        temp3d.mul(t3dRotX);
        temp3d.mul(t3dRotY);
        temp3d.mul(t3dRotZ);
        Matrix4d tempMatrix = new Matrix4d();
        temp3d.get(tempMatrix);
        axisMatrix.mul(tempMatrix);
    }

}
