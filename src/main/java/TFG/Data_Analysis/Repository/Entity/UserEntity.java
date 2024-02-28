package TFG.Data_Analysis.Repository.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Users")
public class UserEntity {
    //region User Attribute
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private long user_id;
    @Column (nullable = false)
    private String name;
    @Column (unique = true, nullable = false)
    private String email;
    @Column (nullable = false)
    private String password;
    @Column (nullable = false, columnDefinition = "boolean default false" )
    private boolean deleted;
    @Column (unique = true)
    private String phone;
    //endregion

    //region Getters & Setters
    public long getUser_id() {
        return user_id;
    }

    public void setUser_id(long user_id) {
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    //endregion
}
