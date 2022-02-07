package host.stjin.anonaddy_shared.models

import android.os.Parcel
import android.os.Parcelable

data class AliasesArray(
    var `data`: ArrayList<Aliases>,
    var links: Links,
    var meta: Meta
)

data class SingleAlias(
    val `data`: Aliases
)

data class Aliases(
    var active: Boolean,
    val aliasable_id: String,
    val aliasable_type: String,
    val created_at: String,
    val deleted_at: String?,
    val description: String?,
    val domain: String,
    val email: String,
    val emails_blocked: Int,
    val emails_forwarded: Int,
    val emails_replied: Int,
    val emails_sent: Int,
    val extension: String,
    val id: String,
    val local_part: String,
    val recipients: List<Recipients>?,
    val updated_at: String,
    val user_id: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.createTypedArrayList(Recipients),
        parcel.readString().toString(),
        parcel.readString().toString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (active) 1 else 0)
        parcel.writeString(aliasable_id)
        parcel.writeString(aliasable_type)
        parcel.writeString(created_at)
        parcel.writeString(deleted_at)
        parcel.writeString(description)
        parcel.writeString(domain)
        parcel.writeString(email)
        parcel.writeInt(emails_blocked)
        parcel.writeInt(emails_forwarded)
        parcel.writeInt(emails_replied)
        parcel.writeInt(emails_sent)
        parcel.writeString(extension)
        parcel.writeString(id)
        parcel.writeString(local_part)
        parcel.writeTypedList(recipients)
        parcel.writeString(updated_at)
        parcel.writeString(user_id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Aliases> {
        override fun createFromParcel(parcel: Parcel): Aliases {
            return Aliases(parcel)
        }

        override fun newArray(size: Int): Array<Aliases?> {
            return arrayOfNulls(size)
        }
    }
}


data class Meta(
    val current_page: Int,
    val from: Int,
    val last_page: Int,
    val links: List<Link>,
    val path: String,
    val per_page: Int,
    val to: Int,
    val total: Int
)

data class Link(
    val active: Boolean,
    val label: String,
    val url: Any
)

data class Links(
    val first: String,
    val last: String,
    val next: Any,
    val prev: Any
)