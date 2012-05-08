package org.openalbum.mixare.marker;

import org.openalbum.mixare.MixContext;
import org.openalbum.mixare.MixState;
import org.openalbum.mixare.gui.PaintScreen;

import android.location.Location;

public interface MarkerInterface {

	public abstract void update(final Location curGPSFix);

	public abstract void draw(final PaintScreen dw);

	public abstract boolean fClick(final float x, final float y,
			final MixContext ctx, final MixState state);

	abstract public int getMaxObjects();

	public abstract String getID();

	public abstract boolean isActive();

}