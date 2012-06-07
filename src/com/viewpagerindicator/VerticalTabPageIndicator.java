/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2011 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viewpagerindicator;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.PagerAdapter;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * This widget implements the dynamic action bar tab behavior that can change
 * across different configurations or circumstances.
 */
public class VerticalTabPageIndicator extends ScrollView implements PageIndicator {

	private Runnable mTabSelector;
	private TitleProvider mAdapter;

	private OnClickListener mTabClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			if (!mPagingEnabled) return;
			TabView tabView = (TabView) view;
			mViewPager.setCurrentItem(tabView.getIndex());
		}
	};

	private LinearLayout mTabLayout;
	private ExtendedViewPager mViewPager;
	private ExtendedViewPager.OnPageChangeListener mListener;

	private LayoutInflater mInflater;

	int mMaxTabWidth;
	private int mSelectedTabIndex;

	private boolean mPagingEnabled = true;

	public VerticalTabPageIndicator(Context context) {
		this(context, null);
	}

	@SuppressWarnings("deprecation")
	public VerticalTabPageIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
		setVerticalScrollBarEnabled(false);

		mInflater = LayoutInflater.from(context);

		mTabLayout = new LinearLayout(getContext()) {

			{
				setOrientation(LinearLayout.VERTICAL);
			}
		};
		addView(mTabLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.FILL_PARENT));
	}

	@Override
	public void notifyDataSetChanged() {
		mTabLayout.removeAllViews();
		mAdapter = (TitleProvider) mViewPager.getAdapter();
		final int count = ((PagerAdapter) mAdapter).getCount();
		for (int i = 0; i < count; i++) {
			String title = mAdapter.getTitle(i);
			Integer icon = mAdapter.getIcon(i);
			if (title != null && icon != null) {
				addTab(title, icon, i);
			} else if (title == null && icon != null) {
				addTab(icon, i);
			} else if (title != null && icon == null) {
				addTab(title, i);
			}
		}
		if (mSelectedTabIndex > count) {
			mSelectedTabIndex = count - 1;
		}
		setCurrentItem(mSelectedTabIndex);
		requestLayout();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (mTabSelector != null) {
			// Re-post the selector we saved
			post(mTabSelector);
		}
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mTabSelector != null) {
			removeCallbacks(mTabSelector);
		}
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		final boolean lockedExpanded = widthMode == MeasureSpec.EXACTLY;
		setFillViewport(lockedExpanded);

		final int childCount = mTabLayout.getChildCount();
		if (childCount > 1 && (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST)) {
			if (childCount > 2) {
				mMaxTabWidth = (int) (MeasureSpec.getSize(widthMeasureSpec) * 0.4f);
			} else {
				mMaxTabWidth = MeasureSpec.getSize(widthMeasureSpec) / 2;
			}
		} else {
			mMaxTabWidth = -1;
		}

		final int oldWidth = getMeasuredWidth();
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int newWidth = getMeasuredWidth();

		if (lockedExpanded && oldWidth != newWidth) {
			// Recenter the tab display if we're at a new (scrollable) size.
			setCurrentItem(mSelectedTabIndex);
		}
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		if (mListener != null) {
			mListener.onPageScrolled(arg0, arg1, arg2);
		}
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		if (mListener != null) {
			mListener.onPageScrollStateChanged(arg0);
		}
	}

	@Override
	public void onPageSelected(int arg0) {
		setCurrentItem(arg0);
		if (mListener != null) {
			mListener.onPageSelected(arg0);
		}
	}

	@Override
	public void setCurrentItem(int item) {
		if (mViewPager == null) throw new IllegalStateException("ExtendedViewPager has not been bound.");

		mViewPager.setCurrentItem(item);
		mSelectedTabIndex = item;
		final int tabCount = mTabLayout.getChildCount();
		for (int i = 0; i < tabCount; i++) {
			final View child = mTabLayout.getChildAt(i);
			final boolean isSelected = i == item;
			child.setSelected(isSelected);
			if (isSelected) {
				animateToTab(item);
			}
		}
	}

	@Override
	public void setOnPageChangeListener(ExtendedViewPager.OnPageChangeListener listener) {
		mListener = listener;
	}

	@Override
	public void setPagingEnabled(boolean enabled) {
		mViewPager.setPagingEnabled(enabled);
		mPagingEnabled = enabled;
	}

	@Override
	public void setViewPager(ExtendedViewPager view) {
		final PagerAdapter adapter = view.getAdapter();
		if (adapter == null) throw new IllegalStateException("ExtendedViewPager does not have adapter instance.");
		if (!(adapter instanceof TitleProvider))
			throw new IllegalStateException(
					"ExtendedViewPager adapter must implement TitleProvider to be used with TitlePageIndicator.");
		mViewPager = view;
		view.setOnPageChangeListener(this);
		notifyDataSetChanged();
	}

	@Override
	public void setViewPager(ExtendedViewPager view, int initialPosition) {
		setViewPager(view);
		setCurrentItem(initialPosition);
	}

	@SuppressWarnings("deprecation")
	private void addTab(int icon, int index) {
		// Workaround for not being able to pass a defStyle on pre-3.0
		final TabView tabView = (TabView) mInflater.inflate(R.layout.vpi__tab_vertical, null);
		tabView.init(this, icon, index);
		tabView.setFocusable(true);
		tabView.setOnClickListener(mTabClickListener);

		mTabLayout.addView(tabView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1));
	}

	@SuppressWarnings("deprecation")
	private void addTab(String text, int index) {
		// Workaround for not being able to pass a defStyle on pre-3.0
		final TabView tabView = (TabView) mInflater.inflate(R.layout.vpi__tab_vertical, null);
		tabView.init(this, text, index);
		tabView.setFocusable(true);
		tabView.setOnClickListener(mTabClickListener);

		mTabLayout.addView(tabView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1));
	}

	@SuppressWarnings("deprecation")
	private void addTab(String text, int icon, int index) {
		// Workaround for not being able to pass a defStyle on pre-3.0
		final TabView tabView = (TabView) mInflater.inflate(R.layout.vpi__tab_vertical, null);
		tabView.init(this, text, icon, index);
		tabView.setFocusable(true);
		tabView.setOnClickListener(mTabClickListener);

		mTabLayout.addView(tabView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1));
	}

	private void animateToTab(final int position) {
		final View tabView = mTabLayout.getChildAt(position);
		if (mTabSelector != null) {
			removeCallbacks(mTabSelector);
		}
		mTabSelector = new Runnable() {

			@Override
			public void run() {
				final int scrollPos = tabView.getTop() - (getHeight() - tabView.getHeight()) / 2;
				smoothScrollTo(0, scrollPos);
				mTabSelector = null;
			}
		};
		post(mTabSelector);
	}

	public static class TabView extends LinearLayout {

		private VerticalTabPageIndicator mParent;
		private int mIndex;

		public TabView(Context context, AttributeSet attrs) {
			super(context, attrs);
			setOrientation(LinearLayout.VERTICAL);
		}

		public int getIndex() {
			return mIndex;
		}

		public void init(VerticalTabPageIndicator parent, int icon, int index) {
			mParent = parent;
			mIndex = index;

			ImageView imageView = (ImageView) findViewById(android.R.id.icon);
			imageView.setVisibility(View.VISIBLE);
			imageView.setImageResource(icon);

			VerticalTextView textView = (VerticalTextView) findViewById(android.R.id.text1);
			textView.setVisibility(View.GONE);
		}

		public void init(VerticalTabPageIndicator parent, String text, int index) {
			mParent = parent;
			mIndex = index;

			ImageView imageView = (ImageView) findViewById(android.R.id.icon);
			imageView.setVisibility(View.GONE);

			VerticalTextView textView = (VerticalTextView) findViewById(android.R.id.text1);
			textView.setVisibility(View.VISIBLE);
			textView.setText(text);
		}

		public void init(VerticalTabPageIndicator parent, String text, int icon, int index) {
			mParent = parent;
			mIndex = index;

			ImageView imageView = (ImageView) findViewById(android.R.id.icon);
			imageView.setVisibility(View.VISIBLE);
			imageView.setImageResource(icon);

			VerticalTextView textView = (VerticalTextView) findViewById(android.R.id.text1);
			textView.setVisibility(View.VISIBLE);
			textView.setText(text);
		}

		@Override
		public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

			// Re-measure if we went beyond our maximum size.
			if (mParent.mMaxTabWidth > 0 && getMeasuredWidth() > mParent.mMaxTabWidth) {
				super.onMeasure(MeasureSpec.makeMeasureSpec(mParent.mMaxTabWidth, MeasureSpec.EXACTLY),
						heightMeasureSpec);
			}
		}
	}

	public static class VerticalTextView extends TextView {

		final boolean topDown;

		public VerticalTextView(Context context, AttributeSet attrs) {
			super(context, attrs);
			final int gravity = getGravity();
			if (Gravity.isVertical(gravity) && (gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
				setGravity(gravity & Gravity.HORIZONTAL_GRAVITY_MASK | Gravity.TOP);
				topDown = false;
			} else {
				topDown = true;
			}
		}

		@Override
		protected void onDraw(Canvas canvas) {
			TextPaint textPaint = getPaint();
			textPaint.setColor(getCurrentTextColor());
			textPaint.drawableState = getDrawableState();

			canvas.save();

			if (topDown) {
				canvas.translate(getWidth(), 0);
				canvas.rotate(90);
			} else {
				canvas.translate(0, getHeight());
				canvas.rotate(-90);
			}

			canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());

			getLayout().draw(canvas);
			canvas.restore();
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(heightMeasureSpec, widthMeasureSpec);
			setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
		}
	}
}
