package edu.sb.poker.persistence;

import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.eclipse.persistence.annotations.CacheIndex;
import edu.sb.poker.util.Copyright;
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
@Table(schema = "poker", name = "Document")
@PrimaryKeyJoinColumn(name = "documentIdentity")
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
@XmlType @XmlRootElement
@Copyright(year=2005, holders="Sascha Baumeister")
public class Document extends BaseEntity {

	@NotNull @Size(min = 64, max = 64)
	@Column(nullable = false, updatable = false, insertable = true, length = 64, unique = true)
	@CacheIndex(updateable = false)
	private String hash;

	@NotNull @Size(min = 1, max = 63) @Pattern(regexp = "^[a-z]+/[[a-z][0-9]\\-\\+\\.]+$")
	@Column(nullable = false, updatable = true, length = 63)
	private String type;

	@NotNull @Size(min = 1, max = 16777215)
	@Column(nullable = false, updatable = false, insertable = true)
	private byte[] content;
	
	@OneToMany(mappedBy = "avatar")
	private Set<Person> people;

	/**
	 * Initializes a new instance for JPA, JSON-B and JAX-B.
	 */
	protected Document () {
		this(null);
	}


	/**
	 * Initializes a new instance.
	 * @param content the content
	 */
	public Document (final byte[] content) {
		this.hash = HashCodes.sha2HashText(256, content);
		this.type = "application/octet-stream";
		this.content = content;
	}


	/**
	 * Returns the hash.
	 * @return the 64 character long quasi-unique SHA-256 hash of the content
	 */
	@JsonbProperty @XmlAttribute
	public String getHash () {
		return this.hash;
	}


	/**
	 * Sets the hash.
	 * @param hash the 64 character long quasi-unique SHA-256 hash of the content
	 */
	protected void setHash (final String hash) {
		this.hash = hash;
	}


	/**
	 * Returns the type.
	 * @return the content type
	 */
	@JsonbProperty @XmlAttribute
	public String getType () {
		return this.type;
	}


	/**
	 * Sets the type.
	 * @param type the content type
	 */
	protected void setType (final String type) {
		this.type = type;
	}


	/**
	 * Returns the content.
	 * @return the content
	 */
	@JsonbTransient  @XmlTransient
	public byte[] getContent () {
		return this.content;
	}
}