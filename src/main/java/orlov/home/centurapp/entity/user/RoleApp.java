package orlov.home.centurapp.entity.user;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"roleName"})
public class RoleApp implements GrantedAuthority {
    private int roleId;
    private String roleName;

    @Override
    public String getAuthority() {
        return this.roleName;
    }
}
