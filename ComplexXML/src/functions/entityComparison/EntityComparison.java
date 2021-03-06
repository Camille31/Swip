package functions.entityComparison;

import org.semanticweb.owlapi.model.OWLEntity;

import exception.ComplexMappingException;
import functions.Function;

/**
 *
 * @author Dominique Ritze
 *
 * Interface for all entity comparison functions, e.g.
 * subclass or domain, where two OWLEntities are necessary for the computation.
 *
 */
public interface EntityComparison extends Function{

        /**
         * Computation method for all entity comparisons.
         *
         * @param <T>
         * @param c1
         * @param c2
         * @return
         */
	public boolean compute(OWLEntity c1, OWLEntity c2) throws ComplexMappingException;
}
