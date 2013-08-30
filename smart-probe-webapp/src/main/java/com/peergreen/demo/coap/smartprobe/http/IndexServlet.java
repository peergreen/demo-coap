/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 * Proprietary and confidential.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.peergreen.demo.coap.smartprobe.http;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Element;

/**
 * User: guillaume
 * Date: 30/08/13
 * Time: 12:12
 */
@WebServlet("/")
public class IndexServlet extends AbstractSmartProbeServlet {
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

        Architecture architecture = (Architecture) tracker.getService();
        if (architecture == null) {
            throw new ServletException("SmartProbe instance not available on the system");
        }

        InstanceDescription description = architecture.getInstanceDescription();

        Element intervalElement = findProperty(description.getDescription(), "interval");
        req.setAttribute("interval", intervalElement.getAttribute("value"));

        Element uriElement = findProperty(description.getDescription(), "smarthing-uri");
        req.setAttribute("uri", uriElement.getAttribute("value"));

        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/configure.jspx");
        dispatcher.forward(req, resp);
    }

    private Element findProperty(final Element description, final String propertyName) {
        if ("property".equals(description.getName()) && propertyName.equals(description.getAttribute("name"))) {
            return description;
        }

        for (Element element : description.getElements()) {
            Element found = findProperty(element, propertyName);
            if (found != null) {
                return found;
            }
        }

        return null;
    }
}
