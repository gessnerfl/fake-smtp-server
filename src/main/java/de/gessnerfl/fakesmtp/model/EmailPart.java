package de.gessnerfl.fakesmtp.model;

import javax.persistence.*;

@MappedSuperclass
public abstract class EmailPart {
    @Id
    @GeneratedValue(generator = "email_part_generator", strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email")
    private Email email;

    @Lob
    @Column(name = "data", nullable = false)
    @Basic(optional = false)
    private String data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
