package prontuario.al.test.annotations

import com.netflix.graphql.dgs.test.EnableDgsTest
import org.springframework.boot.test.context.SpringBootTest

@Target(AnnotationTarget.CLASS)
@SpringBootTest
@EnableDgsTest
annotation class GraphQLTest
