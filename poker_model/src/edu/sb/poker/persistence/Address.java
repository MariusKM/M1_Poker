package edu.sb.poker.persistence;

import java.util.Comparator;

import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import edu.sb.poker.util.JsonProtectedPropertyStrategy;

@Embeddable
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
public class Address implements Comparable<Address> {
	static private final Comparator<Address> COMPARATOR = Comparator.comparing(Address::getCity).thenComparing(Address::getPostcode);
	
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

	public String getStreet() {
		return street;
	}
	
	protected void setStreet(String street) {
		this.street = street;
	}

	public String getPostcode() {
		return postcode;
	}
	
	protected void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public String getCity() {
		return city;
	}
	
	protected void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	protected void setCountry(String country) {
		this.country = country;
	}

	@Override
	public int compareTo(Address other) {

	return COMPARATOR.compare(this,other);
	}

	
}
