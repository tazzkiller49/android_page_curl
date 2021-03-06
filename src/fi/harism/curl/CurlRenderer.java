package fi.harism.curl;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;

/**
 * Actual renderer class. Multiple bitmaps should be provided to get proper
 * results.
 * 
 * @author harism
 */
public class CurlRenderer implements GLSurfaceView.Renderer {

	public static final int SHOW_ONE_PAGE = 1;
	public static final int SHOW_TWO_PAGES = 2;

	public static final int PAGE_LEFT = 1;
	public static final int PAGE_RIGHT = 2;

	private int mViewMode = SHOW_ONE_PAGE;
	// Rect for render area.
	private RectF mViewRect = new RectF();
	// Screen size.
	private int mViewportWidth;
	private int mViewportHeight;

	// Curl meshes used for static and dynamic rendering.
	private Vector<CurlMesh> mCurlMeshes;

	private boolean mBackgroundColorChanged = false;
	private int mBackgroundColor;

	private CurlRendererObserver mObserver;

	private RectF mCurlRectLeft;
	private RectF mCurlRectRight;

	/**
	 * Basic constructor.
	 */
	public CurlRenderer(CurlRendererObserver observer) {
		mObserver = observer;
		mCurlMeshes = new Vector<CurlMesh>();
		mCurlRectLeft = new RectF();
		mCurlRectRight = new RectF();
	}

	/**
	 * Adds CurlMesh to this renderer.
	 */
	public synchronized void addCurlMesh(CurlMesh mesh) {
		removeCurlMesh(mesh);
		mCurlMeshes.add(mesh);
	}

	/**
	 * Returns rect reserved for left or right page.
	 */
	public RectF getPageRect(int page) {
		if (page == PAGE_LEFT) {
			return mCurlRectLeft;
		} else if (page == PAGE_RIGHT) {
			return mCurlRectRight;
		}
		return null;
	}

	/**
	 * Translates screen coordinates into view coordinates.
	 */
	public PointF getPos(float x, float y) {
		PointF ret = new PointF();
		ret.x = mViewRect.left + (mViewRect.width() * x / mViewportWidth);
		ret.y = mViewRect.top - (-mViewRect.height() * y / mViewportHeight);
		return ret;
	}

	/**
	 * Getter for current view mode.
	 */
	public int getViewMode() {
		return mViewMode;
	}

	@Override
	public synchronized void onDrawFrame(GL10 gl) {
		if (mBackgroundColorChanged) {
			gl.glClearColor(Color.red(mBackgroundColor) / 255f,
					Color.green(mBackgroundColor) / 255f,
					Color.blue(mBackgroundColor) / 255f,
					Color.alpha(mBackgroundColor) / 255f);
		}

		gl.glClear(GL10.GL_COLOR_BUFFER_BIT); // | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		for (int i = 0; i < mCurlMeshes.size(); ++i) {
			mCurlMeshes.get(i).draw(gl);
		}

		mObserver.onRenderDone();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);
		mViewportWidth = width;
		mViewportHeight = height;

		float ratio = (float) width / height;
		mViewRect.top = 1.0f;
		mViewRect.bottom = -1.0f;
		mViewRect.left = -ratio;
		mViewRect.right = ratio;
		setViewMode(mViewMode);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU.gluOrtho2D(gl, mViewRect.left, mViewRect.right, mViewRect.bottom,
				mViewRect.top);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		setBackgroundColor(0xFF303030);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	}

	/**
	 * Removes CurlMesh from this renderer.
	 */
	public synchronized void removeCurlMesh(CurlMesh mesh) {
		while (mCurlMeshes.remove(mesh))
			;
	}

	/**
	 * Change background/clear color.
	 */
	public void setBackgroundColor(int color) {
		mBackgroundColor = color;
		mBackgroundColorChanged = true;
	}

	/**
	 * Sets visible page count to one or two.
	 */
	public synchronized void setViewMode(int viewmode) {
		if (viewmode == SHOW_ONE_PAGE) {
			mViewMode = viewmode;
			mCurlRectRight.set(mViewRect);
			mObserver.onBitmapSizeChanged(mViewportWidth, mViewportHeight);
		} else if (viewmode == SHOW_TWO_PAGES) {
			mViewMode = viewmode;
			mCurlRectLeft.set(mViewRect);
			mCurlRectLeft.right = 0;
			mCurlRectRight.set(mViewRect);
			mCurlRectRight.left = 0;
			mObserver.onBitmapSizeChanged((mViewportWidth + 1) / 2,
					mViewportHeight);
		}
	}

	/**
	 * Observer for waiting render engine/state updates.
	 */
	public interface CurlRendererObserver {
		public void onBitmapSizeChanged(int width, int height);

		public void onRenderDone();
	}
}
