package health.ere.ps.model.dgc;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Objects;

/**
 * Single recovery data entry.
 */
public class RecoveryEntry {
    @JsonProperty("id")
    private String id = null;

    @JsonProperty("tg")
    private String tg = null;

    @JsonProperty("fr")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private String fr = null;

    @JsonProperty("df")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private String df = null;

    @JsonProperty("du")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private String du = null;

    /**
     * Identifier of the health professional location (i.e. BSNR or similar identifer).
     * It will be used in the construction of the DGCI (digitial green certificate identifier).
     * Due to the specification of the DGCI only the use of uppercase letters and numbers 0-9 are allowed.
     *
     * @return id
     **/
    @JsonProperty("id")
    @NotNull
    @Pattern(regexp = "^[0-9A-Z]+$")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get tg
     *
     * @return tg
     **/
    @JsonProperty("tg")
    @NotNull
    @Valid
    public String getTg() {
        return tg;
    }

    public void setTg(String tg) {
        this.tg = tg;
    }

    /**
     * First positive test result date as ISO 8601.
     *
     * @return fr
     **/
    @JsonProperty("fr")
    @NotNull
    @Valid
    public String getFr() {
        return fr;
    }

    public void setFr(String fr) {
        this.fr = fr;
    }

    /**
     * Certificate valid from date as ISO 8601.
     *
     * @return df
     **/
    @JsonProperty("df")
    @NotNull
    @Valid
    public String getDf() {
        return df;
    }

    public void setDf(String df) {
        this.df = df;
    }

    /**
     * Certificate valid until date as ISO 8601.
     *
     * @return du
     **/
    @JsonProperty("du")
    @NotNull
    @Valid
    public String getDu() {
        return du;
    }

    public void setDu(String du) {
        this.du = du;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RecoveryEntry recoveryEntry = (RecoveryEntry) o;
        return Objects.equals(this.id, recoveryEntry.id) &&
                Objects.equals(this.tg, recoveryEntry.tg) &&
                Objects.equals(this.fr, recoveryEntry.fr) &&
                Objects.equals(this.df, recoveryEntry.df) &&
                Objects.equals(this.du, recoveryEntry.du);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tg, fr, df, du);
    }


    @Override
    public String toString() {

        return "class RecoveryEntry {\n" +
                "    id: " + id + "\n" +
                "    tg: " + tg + "\n" +
                "    fr: " + fr + "\n" +
                "    df: " + df + "\n" +
                "    du: " + du + "\n" +
                "}";
    }
}
