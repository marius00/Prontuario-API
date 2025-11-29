package prontuario.al.dummy

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import prontuario.al.generated.types.DummyInput
import prontuario.al.generated.types.DummyOutput
import java.util.*

@DgsComponent
class DummyResolver(
) {
    @DgsQuery
    fun dummyQuery(
        @InputArgument input: DummyInput,
    ): DummyOutput {
        return DummyOutput(input.name.uppercase(Locale.getDefault()), "", input.sum)
    }
}
