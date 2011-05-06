package codeminders.yfrog.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateEx extends Date {

    private int _year, _month, _dayOfMonth,
        _hours, _minutes, _seconds, _milliseconds;

    public DateEx() {
        super();
        unpack();
    }

    public DateEx(long date) {
        super(date);
        unpack();
    }

    public void setTime(long time) {
        super.setTime(time);
        unpack();
    }

    public int getYear() { return _year; }
    public int getMonth() { return _month; }
    public int getDayOfMonth() { return _dayOfMonth; }
    public int getHours() { return _hours; }
    public int getMinutes() { return _minutes; }
    public int getSeconds() { return _seconds; }
    public int getMilliseconds() { return _milliseconds; }

    public void set(int year, int month, int dayOfMonth, int hours, int minutes, int seconds, int milliseconds) {
        _year = year;
        _month = month;
        _dayOfMonth = dayOfMonth;
        _hours = hours;
        _minutes = minutes;
        _seconds = seconds;
        _milliseconds = milliseconds;
        pack();
    }
    public void setYear(int value) { _year = value; pack(); }
    public void setMonth(int value) { _month = value; pack(); }
    public void setDayOfMonth(int value) { _dayOfMonth = value; pack(); }
    public void setHours(int value) { _hours = value; pack(); }
    public void setMinutes(int value) { _minutes = value; pack(); }
    public void setSeconds(int value) { _seconds = value; pack(); }
    public void setMilliseconds(int value) { _milliseconds = value; pack(); }

    private void unpack() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(this);
        _year = c.get(Calendar.YEAR);
        _month = c.get(Calendar.MONTH);
        _dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
        _hours = c.get(Calendar.HOUR_OF_DAY);
        _minutes = c.get(Calendar.MINUTE);
        _seconds = c.get(Calendar.SECOND);
        _milliseconds = c.get(Calendar.MILLISECOND);
    }

    private void pack() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(new Date(0));
        c.set(Calendar.YEAR, _year);
        c.set(Calendar.MONTH, _month);
        c.set(Calendar.DAY_OF_MONTH, _dayOfMonth);
        c.set(Calendar.HOUR_OF_DAY, _hours);
        c.set(Calendar.MINUTE, _minutes);
        c.set(Calendar.SECOND, _seconds);
        c.set(Calendar.MILLISECOND, _milliseconds);
        super.setTime(c.getTime().getTime());
    }
}

