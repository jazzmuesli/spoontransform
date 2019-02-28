package org.pavelreich.saaremaa;

public abstract class ProjectEntry {

	public abstract String getName();

	@Override
	public String toString() {
		return getClass().getSimpleName()+"[" + getName() + "]";
	}

	protected abstract boolean isDirectory();

	protected abstract long getSize();

	protected abstract long getTime();
}
