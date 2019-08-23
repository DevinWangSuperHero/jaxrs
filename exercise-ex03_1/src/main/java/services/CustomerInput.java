package services;

import lombok.Getter;

import javax.ws.rs.PathParam;

@Getter
public class CustomerInput {

    @PathParam("id")
    int id;
}
