package edu.sb.poker.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;


/**
 * <i>JAX-B</i> adapter that marshals Long instances into String, and vice versa.
 */
@Copyright(year = 2010, holders = "Sascha Baumeister")
public class XmlLongToStringAdapter extends XmlAdapter<String,Long> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String marshal (final Long value) {
		return value == null ? null : value.toString();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long unmarshal (final String value) throws NumberFormatException {
		return value == null ? null : Long.parseLong(value);
	}
}