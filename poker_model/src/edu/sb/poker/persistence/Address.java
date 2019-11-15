package edu.sb.poker.persistence;

import static javax.xml.bind.annotation.XmlAccessType.NONE;

import java.util.Comparator;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import edu.sb.poker.util.JsonProtectedPropertyStrategy;

@Embeddable
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
@XmlAccessorType(NONE)
@XmlType @XmlRootElement
public class Address implements Comparable<Address> {
	static private final Comparator<Address> COMPARATOR = Comparator.comparing(Address::getCountry)
			.thenComparing(Address::getPostcode)
			.thenComparing(Address::getCity)
			.thenComparing(Address::getStreet);
	
	@Column(nullable = false, updatable = true)
	@NotNull @Size(min=0, max=63)
	private String street;
	
	@Column(nullable = false, updatable = true)
	@NotNull @Size(min=0, max=15)
	private String postcode;
	
	@Column(nullable = false, updatable = true)
	@NotNull @Size(min=1, max=63)
	private String city;
	
	@Column(nullable = false, updatable = true)
	@NotNull @Size(min=1, max=63)
	private String country;

	@JsonbProperty @XmlAttribute
	public String getStreet() {
		return street;
	}
	
	public void setStreet(String street) {
		this.street = street;
	}

	@JsonbProperty @XmlAttribute
	public String getPostcode() {
		return postcode;
	}
	
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	@JsonbProperty @XmlAttribute
	public String getCity() {
		return city;
	}
	
	public void setCity(String city) {
		this.city = city;
	}

	@JsonbProperty @XmlAttribute
	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@Override
	public int compareTo(Address other) {

	return COMPARATOR.compare(this,other);
	}

	
}