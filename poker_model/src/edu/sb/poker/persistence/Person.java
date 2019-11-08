package edu.sb.poker.persistence;

import java.util.HashSet;
import java.util.Set;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import edu.sb.poker.util.HashCodes;
import edu.sb.poker.util.JsonProtectedPropertyStrategy;


/**
 * This class models document entities based on database rows, representing a kind of embedded content management system,
 * similarly to what was defined in JSR086 for EJB2. The documents use SHA-256 content hashes as natural document keys, thereby
 * preventing multiple entities storing the same content. The document entities do not track their inverse relationships, as
 * these should be of little general interest. Note that some special considerations must be taken into account when dealing
 * with any kind of large object binary (LOB) column in an object-relational model:
 * <ul>
 * <li>Mapping content into databases has the general advantage over simple file system storage of guaranteeing consistency when
 * performing ACID transactions or database backups.</li>
 * <li>The primary choice is mapping the content to byte arrays, which combines simplicity with effective 2nd level caching of
 * document content. However, content size must be carefully considered in this case, in order to avoid excessive memory demands
 * in a server environment: Content under one MB in size should usually be ok; 64KB is the default size usually reserved for
 * streaming I/O buffers and IP packets, which indicates the super safe lower limit. Larger content should also be bearable if
 * it isn't constantly accessed, maybe up to a few MB in size; a 16MB MEDIUMBLOB cell still fits around 256 times into 4GB of
 * memory. However, once content size exceeds these limits, direct mapping into byte arrays quickly becomes questionable.</li>
 * <li>If byte arrays cannot be used for the complete content because the content size is too large, an interesting alternative
 * is to break up the content into smaller chunks, for example stored in 32KB VARBINARY fields of 1:* related "DocumentChunk"
 * entities. Using JP-QL queries for the chunk identities, the chunks can be accessed serially whenever the content is required,
 * while still maintaining the effectiveness of the 2nd level cache if desired.</li>
 * <li>In opposition to this, JPA mapping to {@code java.sql.Blob} (which promises streaming I/O) is not supported by most JPA
 * implementations. The reasons for this are multi-faceted: Few databases really support server-side streaming I/O of BLOBs;
 * MySQL for example does not and probably never will. Even fewer JDBC connector implementations really support streaming I/O of
 * BLOBs, just copying the whole content before granting read I/O access, implicitly defeating the motivation of using
 * {@code Blob} in the first place. On top of this, {@code Blob} instances must remain connected to the database while their
 * content is accessed, which can easily cause issues with resource closing. And on top of this, the content cannot become part
 * of the 2nd level cache, which implies it has to be transported over a JDBC connection for every request, potentially crowding
 * the latter with traffic. Therefore, JPA {@code Blob} mapping support is shaky at best, and seldom recommended.</li>
 * <li>When an application grows into a serious business, the time will come when there needs to be a decision if the content
 * shall be migrated into a dedicated content management system or content distribution network. However, keep in mind that
 * content management systems are usually based on databases; what's primarily being sold (usually for lots of money) is
 * know-how of how to use a database to manage content efficiently. There is nothing inherently "magic" about them, apart from
 * representing a billion Dollar a year cash cow for some manufacturers.</li>
 * </ul>
 */

@Entity
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
@Table(schema = "poker", name = "Person", uniqueConstraints = @UniqueConstraint(columnNames = { "pokerTableReference", "position" }))
@PrimaryKeyJoinColumn(name = "personIdentity")
public class Person extends BaseEntity {
	public enum Group {
		ADMIN, USER;
	}

	static private final String DEFAULT_PASSWORD_HASH = HashCodes.sha2HashText(256, "password");

	@Column(nullable = false, updatable = true, unique = true)
	@NotNull
	@Size(min = 1, max = 128)
	@Email
	private String email;

	@Column(nullable = false, updatable = true)
	@NotNull
	@Size(min = 64, max = 64)
	private String passwordHash;

	@Column(nullable = false, updatable = true)
	@PositiveOrZero
	private long balance;

	@Enumerated(EnumType.STRING)
	@Column(name = "groupAlias", nullable = false, updatable = true)
	@NotNull
	private Group group;

	@ElementCollection
	@CollectionTable(schema = "poker", name = "PersonPhoneAssociation", joinColumns = @JoinColumn(name = "personReference", nullable = false, updatable = true), uniqueConstraints = @UniqueConstraint(columnNames = { "personReference", "phone" }))
	@NotNull
	private Set<String> phones;

	@Embedded
	@NotNull
	@Valid
	@AttributeOverrides({ @AttributeOverride(name = "title", column = @Column(name = "title")), @AttributeOverride(name = "family", column = @Column(name = "surname")), @AttributeOverride(name = "given", column = @Column(name = "forename")), })
	private Name name;

	@Embedded
	@NotNull
	@Valid
	@AttributeOverrides({ @AttributeOverride(name = "street", column = @Column(name = "street")), @AttributeOverride(name = "postcode", column = @Column(name = "postcode")), @AttributeOverride(name = "city", column = @Column(name = "city")), @AttributeOverride(name = "country", column = @Column(name = "country")), })
	private Address address;

	@Embedded
	@Valid
	@AttributeOverrides({ @AttributeOverride(name = "offer", column = @Column(name = "negotiationOffer")), @AttributeOverride(name = "answer", column = @Column(name = "negotiationAnswer")), @AttributeOverride(name = "timestamp", column = @Column(name = "negotiationTimestamp")), })
	private Negotiation negotiation;

	@Column(nullable = true, updatable = true)
	@PositiveOrZero
	private Byte position;

	// TODO: on delete restrict
	@ManyToOne(optional = true, cascade = { CascadeType.REFRESH, CascadeType.MERGE, CascadeType.DETACH })
	@JoinColumn(name = "pokerTableReference", nullable = true, updatable = true)
	private PokerTable table;

	// TODO: on delete restrict
	@ManyToOne(optional = false, cascade = { CascadeType.REFRESH, CascadeType.MERGE, CascadeType.DETACH })
	@JoinColumn(name = "avatarReference", nullable = false, updatable = true)
	private Document avatar;

	// TODO: why no reference to Hand?

	public Person () {
		super();
		this.passwordHash = DEFAULT_PASSWORD_HASH;
		this.group = Group.USER;
		this.name = new Name();
		this.address = new Address();
		this.phones = new HashSet<>();
	}


	public Name getName () {
		return null;
	}


	protected void setName (Name name) {
		this.name = name;
	}


	public Address getAddress () {
		return address;
	}


	protected void setAddress (Address address) {
		this.address = address;
	}


	public Negotiation getNegotiation () {
		return negotiation;
	}


	protected void setNegotiation (Negotiation negotiation) {
		this.negotiation = negotiation;
	}


	public String getEmail () {
		return email;
	}


	protected void setEmail (String email) {
		this.email = email;
	}


	public PokerTable getPoker () {
		return this.table;
	}


	protected void setPoker (PokerTable table) {
		this.table = table;
	}


	public Group getGroup () {
		return group;
	}


	public void setGroup (Group group) {
		this.group = group;
	}


	public String getPasswordHash () {
		return passwordHash;
	}


	public void setPasswordHash (String passwordHash) {
		this.passwordHash = passwordHash;
	}


	public Set<String> getPhones () {
		return phones;
	}


	protected void setPhones (Set<String> phones) {
		this.phones = phones;
	}


	public long getBalance () {
		return balance;
	}


	protected void setBalance (long balance) {
		this.balance = balance;
	}


	public Byte getPosition () {
		return position;
	}


	protected void setPosition (Byte position) {
		this.position = position;
	}


	@JsonbProperty
	protected long getAvatarReference () {
		return this.avatar == null ? 0 : this.avatar.getIdentity();
	}


	@JsonbProperty
	protected Long getTableReference () {
		return this.table == null ? 0 : this.table.getIdentity();
	}

}