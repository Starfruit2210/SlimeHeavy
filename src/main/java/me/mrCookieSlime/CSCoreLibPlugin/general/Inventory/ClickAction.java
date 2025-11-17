package me.mrCookieSlime.CSCoreLibPlugin.general.Inventory;

import org.bukkit.event.Cancellable;

/**
 * An old remnant of CS-CoreLib.
 * This will be removed once we updated everything.
 * Don't look at the code, it will be gone soon, don't worry.
 */
public class ClickAction {

    private final boolean right;
    private final boolean left;
    private final boolean shift;
    private final boolean shiftRight;

    public ClickAction(boolean rightClicked, boolean leftClicked, boolean shiftClicked, boolean shiftRightClicked) {
        this.right = rightClicked;
        this.left = leftClicked;
        this.shift = shiftClicked;
        this.shiftRight = shiftRightClicked;
    }

    public boolean isRightClicked() {
        return right;
    }

    public boolean isLeftClicked() {
        return left;
    }

    public boolean isShiftClicked() {
        return shift;
    }

    public boolean isShiftRightClicked() {
        return shiftRight;
    }

}
