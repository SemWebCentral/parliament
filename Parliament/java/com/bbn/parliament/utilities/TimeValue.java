// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.utilities;

/**
 * TimeValue represents time as a (seconds, micro-seconds) tuple.
 * CREDIT:  Doug Schmidt &lt;schmidt@cs.wustl.edu&gt; and the ACE C++ Library
 *
 * @author Paul Neves Created on Nov 5, 2002.
 */
public class TimeValue
{
	public final static long PARLIAMENT_ONE_SECOND_IN_USECS = 1000000L;
	private Long             _sec;
	private Long             _usec;

	public TimeValue()
	{
		set(0, 0);  // Normalization not necessary for (0,0)
	}

	public TimeValue(long sec, long usec)
	{
		set(sec, usec);
		normalize();
	}

	public void set(long sec, long usec)
	{
		_sec = new Long(sec);
		_usec = new Long(usec);
	}

	public void set(double d)
	{
		long l = Double.doubleToLongBits(d);
		_sec = new Long(l);
		_usec = new Long((long) (d - l) * PARLIAMENT_ONE_SECOND_IN_USECS);
		normalize();
	}

	private void normalize()
	{
		long seconds = _sec.longValue();
		long useconds = _usec.longValue();

		// Based on Doug Schmidt's ACE code from Hans Rohnert (P.N.)
		if (useconds >= PARLIAMENT_ONE_SECOND_IN_USECS)
		{
			do
			{
				seconds++;
				useconds -= PARLIAMENT_ONE_SECOND_IN_USECS;
			} while (useconds >= PARLIAMENT_ONE_SECOND_IN_USECS);
		}
		else if (useconds <= -PARLIAMENT_ONE_SECOND_IN_USECS)
		{
			do
			{
				seconds--;
				useconds += PARLIAMENT_ONE_SECOND_IN_USECS;
			} while (useconds <= -PARLIAMENT_ONE_SECOND_IN_USECS);
		}

		if (seconds >= 1 && useconds < 0)
		{
			seconds--;
			useconds += PARLIAMENT_ONE_SECOND_IN_USECS;
		}
		else if (seconds < 0 && useconds > 0)
		{
			seconds++;
			useconds -= PARLIAMENT_ONE_SECOND_IN_USECS;
		}

		_sec = new Long(seconds);
		_usec = new Long(useconds);
	}

	/** Returns the msec. */
	public long getMsec()
	{
		return (_sec.longValue() * 1000) + (_usec.longValue() / 1000);
	}

	/** Returns the sec. */
	public long getSec()
	{
		return _sec.longValue();
	}

	/** Returns the usec. */
	public long getUsec()
	{
		return _usec.longValue();
	}

	/** Sets the sec. */
	public void setSec(long sec)
	{
		_sec = new Long(sec);
	}

	/** Sets the usec. */
	public void setUsec(long usec)
	{
		_usec = new Long(usec);
		normalize();
	}

	/** Sets the msec. */
	public void setMsec(long milliseconds)
	{
		_sec = new Long(milliseconds / 1000);
		_usec = new Long(
			(milliseconds - (_sec.longValue() * 1000)) * 1000);
		normalize();
	}

	public int compareTo(Object obj)
	{
		TimeValue tv = (TimeValue) obj;
		return compareTo(tv);
	}

	public int compareTo(TimeValue anotherTimeValue)
	{
		int retval = 0;

		long diffSec = getSec() - anotherTimeValue.getSec();

		if (diffSec > 0)
		{
			return 1;
		}
		else if (diffSec < 0)
		{
			return -1;
		}

		long diffUsec = getUsec() - anotherTimeValue.getUsec();

		if (diffUsec > 0)
		{
			return 1;
		}
		else if (diffUsec < 0)
		{
			return -1;
		}

		return retval;
	}

	/** @see java.lang.Object#equals(Object) */
	@Override
	public boolean equals(Object obj)
	{
		boolean retval = false;

		if (this == obj)
		{
			return true;
		}

		if (obj instanceof TimeValue)
		{
			TimeValue tv = (TimeValue) obj;

			retval = (getSec() == tv.getSec() && getUsec() == tv.getUsec());
		}

		return retval;
	}

	/** @see java.lang.Object#hashCode() */
	@Override
	public int hashCode()
	{
		return _sec.hashCode() ^ _usec.hashCode();
	}

	/** @see java.lang.Object#toString() */
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("[ sec: " + _sec + " usec: " + _usec + " ]");
		return buf.toString();
	}

	public static void main(String[] args)
	{
		TimeValue tv1 = new TimeValue(100, 1000);
		System.out.println("TimeValue tv1 = " + tv1);

		TimeValue tv2 = new TimeValue(0, PARLIAMENT_ONE_SECOND_IN_USECS * 10 + 1);
		System.out.println("TimeValue tv2 = " + tv2);
	}
}
