package host.stjin.anonaddy_shared.models

import android.os.Parcel
import android.os.Parcelable

data class RecipientsArray(
    val `data`: List<Recipients>
)

data class SingleRecipient(
    val `data`: Recipients
)

data class Recipients(
    val aliases: List<Aliases>?,
    val created_at: String,
    val email: String,
    val email_verified_at: String?,
    val fingerprint: String?,
    val id: String,
    val should_encrypt: Boolean,
    val updated_at: String,
    val user_id: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createTypedArrayList(Aliases),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString().toString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString().toString(),
        parcel.readString().toString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(aliases)
        parcel.writeString(created_at)
        parcel.writeString(email)
        parcel.writeString(email_verified_at)
        parcel.writeString(fingerprint)
        parcel.writeString(id)
        parcel.writeByte(if (should_encrypt) 1 else 0)
        parcel.writeString(updated_at)
        parcel.writeString(user_id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Recipients> {
        override fun createFromParcel(parcel: Parcel): Recipients {
            return Recipients(parcel)
        }

        override fun newArray(size: Int): Array<Recipients?> {
            return arrayOfNulls(size)
        }
    }
}